package com.dshatz.kni.serialization

import com.dshatz.kni.Registry
import com.dshatz.kni.TypeMatcher
import com.dshatz.kni.annotations.JniSerializable
import com.dshatz.kni.annotations.JniSerializerFor
import com.dshatz.kni.kspfix.findAnnotation
import com.dshatz.kni.kspfix.getArgumentValueByName
import com.dshatz.kni.kspfix.getClassArgument
import com.dshatz.kni.utils.dereferenceTypeAlias
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlin.reflect.KClass

class SerializerProcessor(
    private val registry: Registry,
    private val logger: KSPLogger
) {

    private val included = IncludedSerializers(registry, logger)


    fun findSerializables(
        env: SymbolProcessorEnvironment,
        resolver: Resolver
    ): Map<ClassName, SerialClass> {
        val dataClasses = resolver.getSymbolsWithAnnotation(JniSerializable::class.java.name)
            .filterIsInstance<KSClassDeclaration>()
            .filter { d ->
                (d.classKind == ClassKind.CLASS).also {
                    if (!it) env.logger.warn("Ignoring @JniSerializable: not a class", d)
                }
            }
            .filter { d ->
                (Modifier.DATA in d.modifiers).also {
                    if (!it) env.logger.warn("Ignoring @JniSerializable: not a data class", d)
                }
            }

        val withProperties = dataClasses.associate {
            val props = it.primaryConstructor!!.parameters.map {
                SerialProp(
                    it.name?.asString()!!,
                    it.type.dereferenceTypeAlias().toTypeName(),
                    overrideSerializer = it.getOverrideSerializer()
                )
            }
            it.toClassName() to SerialClass(
                it.toClassName(),
                properties = props
            )
        }
        return withProperties
    }

    private fun KSValueParameter.getOverrideSerializer(): ClassName? {
        val paramValue = findAnnotation<JniSerializable>()?.getArgumentValueByName<KClass<*>>("with")
        if (paramValue != null) {
            return (paramValue as KSTypeReference).resolve().toClassName()
        } else return null
    }

    fun findSerializers(resolver: Resolver, env: SymbolProcessorEnvironment): Map<ClassName, ClassName> {
        val serializers = resolver.getSymbolsWithAnnotation(JniSerializerFor::class.java.name)
            .filterIsInstance<KSClassDeclaration>()
        return serializers.mapNotNull {
            if (it.classKind != ClassKind.OBJECT && it.classKind != ClassKind.CLASS) {
                env.logger.error("@JniSerializerFor must be applied to a class/object.", it)
                null
            } else {
                val serializable = it.findAnnotation<JniSerializerFor>()
                    ?.getClassArgument("target") ?: run {
                        env.logger.error("Could not read @JniSerializerFor annotation", it)
                        error("Could not read target argument")
                    }
                serializable to it.toClassName()
            }
        }.toMap()
    }

    fun generateSerializers(
        serializables: Map<ClassName, SerialClass>
    ): List<FileSpec> {
        return serializables.mapNotNull { (type, info) ->
            if (type in IncludedSerializers.kioSupported) {
                // noop
                null
            } else {
                val serializerCls = type.serializerClass()
                val serializer = TypeSpec.objectBuilder(serializerCls)
                    .addSuperinterface(TypeMatcher.JniSerializer.parameterizedBy(type))
                    .addFunction(
                        buildPackFunction(type)
                            .addCode(packCode(info.properties)).build()
                    )
                    .addFunction(
                        buildUnpackFunction(type)
                            .addCode(unpackCode(info)).build()
                    ).build()

                val file = FileSpec.builder(serializerCls)
                    .addType(serializer)
                    .build()
                file
            }
        }
    }

    private fun packCode(props: List<SerialProp>): CodeBlock {
        val builder = CodeBlock.builder()
        props.map {
            val serializer = included.serializer(it.type)
            if (serializer is IncludedSerializers.Serializer.Generic) registry.genericSerializers.add(serializer)
            val statement = serializer.writeCode(CodeBlock.of("buffer"), CodeBlock.of("value.%N", it.name))
            builder.add("\n%L", statement)
        }
        return builder.build()
    }

    private fun unpackCode(info: SerialClass): CodeBlock {
        val builder = CodeBlock.builder()
        info.properties.forEach {
            val serializer = included.serializer(it.type)
            builder.add("val %N = %L\n", it.name, serializer.readCode(CodeBlock.of("buffer")))
        }
        return builder.addStatement("return %T(%L)", info.cls, info.properties.joinToString { it.name }).build()
    }

    private fun buildPackFunction(type: TypeName): FunSpec.Builder {
        return FunSpec.builder("packTo")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                ParameterSpec("value", type)
            )
            .addParameter(
                ParameterSpec("buffer", TypeMatcher.IoBuffer)
            )
    }

    private fun buildUnpackFunction(type: TypeName): FunSpec.Builder {
        return FunSpec.builder("unpackFrom")
            .addModifiers(KModifier.OVERRIDE)
            .returns(type)
            .addParameter(
                ParameterSpec("buffer", TypeMatcher.IoBuffer)
            )
    }

    fun generateGeneric(serializer: IncludedSerializers.Serializer.Generic): GeneratedGenericSerializer {
        val typeParams = serializer.kotlinType.typeArguments.zip(serializer.argumentSerializer)
        val argSerializers = typeParams.mapNotNull { (type, serializer) ->
            if (serializer !is IncludedSerializers.Serializer.StaticObject) {
                val name = "SerializerFor_${(type as ClassName).simpleName}"
                val cls = ClassName(pkg, name)
                registry.serializers[type] = cls
                TypeSpec.objectBuilder(cls)
                    .addSuperinterface(TypeMatcher.JniSerializer.parameterizedBy(type))
                    .addFunction(
                        FunSpec.builder("packTo")
                            .addParameter(ParameterSpec.builder("value", type).build())
                            .addParameter(ParameterSpec.builder("buffer", TypeMatcher.IoBuffer).build())
                            .addModifiers(KModifier.OVERRIDE)
                            .addCode(
                                CodeBlock.of("%L", serializer.writeCode(CodeBlock.of("buffer"),
                                    CodeBlock.of("value")))
                            ).build()
                    )
                    .addFunction(
                        FunSpec.builder("unpackFrom")
                            .addParameter(ParameterSpec.builder("buffer", TypeMatcher.IoBuffer).build())
                            .returns(type)
                            .addModifiers(KModifier.OVERRIDE)
                            .addCode(
                                CodeBlock.of("return %L", serializer.readCode(CodeBlock.of("buffer")))
                            ).build()
                    )
                    .build()
            } else null
        }

        val generic = serializer.kotlinType
        val name = generic.genericSerializerName()
        val typed = PropertySpec.builder(name.simpleName, TypeMatcher.JniSerializer.parameterizedBy(generic))
            .initializer(CodeBlock.of(
                "%T(%L)",
                (serializer.rawSerializer as IncludedSerializers.Serializer.StaticObject).serializer,
                registry.serializers[generic.typeArguments.first()]
            )).build()
        return GeneratedGenericSerializer(
            argSerializers,
            typed
        )
    }

    data class GeneratedGenericSerializer(
        val arg: List<TypeSpec>,
        val serializer: PropertySpec
    )

    fun generateGenericSerializers(): FileSpec {
        val file = FileSpec.builder(pkg, "genericSerializers")
        registry.genericSerializers.map {
            generateGeneric(it)
        }.forEach {
            file.addProperty(it.serializer)
            file.addTypes(it.arg)
        }
        return file.build()
        /*val genericReturns = registry.declarations
            .mapNotNull {
                val genericSerializer = (it.returnType as? TypeInfo.Serializable)?.serializer as? IncludedSerializers.Serializer.Generic
                genericSerializer?.let { s ->
                    it.returnType to s
                }
            }

        val genericArguments = registry.declarations
            .flatMap { f ->
                f.parameters.mapNotNull {
                    val genericSerializer = (it.typeInfo as? TypeInfo.Serializable)?.serializer as? IncludedSerializers.Serializer.Generic
                    genericSerializer?.let { s ->
                        it.typeInfo to s
                    }
                }
            }

        val types = genericReturns + genericArguments
        val fileSpec = FileSpec.builder(pkg, "genericSerializers")
        val paramSerializers = types.flatMap { (type, serializer) ->
            val generic = type.kotlinType as ParameterizedTypeName
            generic.typeArguments.zip(serializer.argumentSerializer)
        }.mapNotNull { (type, serializer) ->
            if (serializer !is IncludedSerializers.Serializer.UserDefined) {
                val name = "SerializerFor_${(type as ClassName).simpleName}"
                val cls = ClassName(pkg, name)
                registry.serializers[type] = cls
                TypeSpec.objectBuilder(cls)
                    .addSuperinterface(TypeMatcher.JniSerializer.parameterizedBy(type))
                    .addFunction(
                        FunSpec.builder("packTo")
                            .addParameter(ParameterSpec.builder("value", type).build())
                            .addParameter(ParameterSpec.builder("buffer", TypeMatcher.IoBuffer).build())
                            .addModifiers(KModifier.OVERRIDE)
                            .addCode(
                                CodeBlock.of("%L", serializer.writeCode(CodeBlock.of("buffer"),
                                    CodeBlock.of("value")))
                            ).build()
                    )
                    .addFunction(
                        FunSpec.builder("unpackFrom")
                            .addParameter(ParameterSpec.builder("buffer", TypeMatcher.IoBuffer).build())
                            .returns(type)
                            .addModifiers(KModifier.OVERRIDE)
                            .addCode(
                                CodeBlock.of("return %L", serializer.readCode(CodeBlock.of("buffer")))
                            ).build()
                    )
                    .build()
            } else null
        }
        val genericSerializers = types.distinctBy { it.first }.map { (type, serializer) ->
            val generic = type.kotlinType as ParameterizedTypeName
            val name = generic.genericSerializerName()
            PropertySpec.builder(name.simpleName, TypeMatcher.JniSerializer.parameterizedBy(generic))
                .initializer(CodeBlock.of(
                    "%T(%L)",
                    (serializer.rawSerializer as IncludedSerializers.Serializer.UserDefined).serializer,
                    registry.serializers[generic.typeArguments.first()]
                )).build()
        }
        return fileSpec.addTypes(paramSerializers).addProperties(genericSerializers).build()*/
    }

    data class SerialClass(
        val cls: ClassName,
        val properties: List<SerialProp>
    )

    data class SerialProp(
        val name: String,
        val type: TypeName,
        val overrideSerializer: ClassName? = null
    )

}

private val pkg = "com.dshatz.kni.generated"

fun ParameterizedTypeName.genericSerializerName(): ClassName {
    val name = "SerializerFor_${rawType.simpleName}_Of_${typeArguments.joinToString("_") { (it as ClassName).simpleName }}"
    return ClassName(pkg, name)
}

fun TypeName.serializerClass(): ClassName {
    this as ClassName
    return ClassName(packageName, simpleName + "Serializer_generated")
}