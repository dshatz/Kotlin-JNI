package com.dshatz.kni.serialization

import com.dshatz.kni.Registry
import com.dshatz.kni.Types
import com.dshatz.kni.annotations.JniSerializable
import com.dshatz.kni.annotations.JniSerializerFor
import com.dshatz.kni.kspfix.findAnnotation
import com.dshatz.kni.kspfix.getClassArgument
import com.dshatz.kni.utils.dereferenceTypeAlias
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
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

class SerializerProcessor(
    private val registry: Registry,
    private val logger: KSPLogger
) {

    private val included = IncludedSerializers(registry, logger)


    fun findSerializables(
        env: SymbolProcessorEnvironment,
        resolver: Resolver
    ): Sequence<SerialClass> {
        fun collectSerialProps(decl: KSClassDeclaration): SerialClass.DataClass {

            val valParams = decl.primaryConstructor!!.parameters.map {
                it.name!!.asString()
            }.toSet()
            val props = decl.getAllProperties()
                .filter { it.simpleName.asString() in valParams }
                .map {
                SerialProp(
                    it.simpleName.asString(),
                    it.type.dereferenceTypeAlias().toTypeName(),
                    declaration = it,
                    overrideSerializer = it.getOverrideSerializer()
                )
            }.toList()
            return SerialClass.DataClass(decl.toClassName(), props)
        }

        val declarations = resolver.getSymbolsWithAnnotation(JniSerializable::class.java.name)
            .filterIsInstance<KSClassDeclaration>()
        val sealed = declarations.filter { d ->
            Modifier.SEALED in d.modifiers
        }.map { d ->
            val subclasses = d.getSealedSubclasses().map { subclass ->
                collectSerialProps(subclass)
            }.toList()
            SerialClass.Polymorphic(
                d.toClassName(),
                subclasses
            )
        }
        val dataClasses = declarations
            .filter { d ->
                (d.classKind == ClassKind.CLASS).also {
                    if (!it) env.logger.warn("Ignoring @JniSerializable: not a class", d)
                }
            }
            .filter { d ->
                (Modifier.DATA in d.modifiers || Modifier.VALUE in d.modifiers).also {
                    if (!it) env.logger.warn("Ignoring @JniSerializable on a class: must have data or value modifiers.", d)
                }
            }
            .map(::collectSerialProps)

        return dataClasses + sealed
    }

    private fun KSPropertyDeclaration.getOverrideSerializer(): ClassName? {
        val annotation = findAnnotation<JniSerializable>()
        val paramValue = annotation?.getClassArgument("with")
        return paramValue
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
        serializables: Sequence<SerialClass>
    ): List<FileSpec> {
        fun SerialClass.DataClass.generateDataClassSerializer(): TypeSpec? {
            return if (cls in IncludedSerializers.kioSupported) {
                // noop
                null
            } else {
                val serializerCls = cls.serializerClass()
                val serializer = TypeSpec.objectBuilder(serializerCls)
                    .addSuperinterface(Types.JniSerializer.parameterizedBy(cls))
                    .addFunction(
                        buildPackFunction(cls)
                            .addCode(packCode(properties)).build()
                    )
                    .addFunction(
                        buildUnpackFunction(cls)
                            .addCode(unpackCode(this)).build()
                    ).build()

                serializer
            }
        }

        fun SerialClass.Polymorphic.generatePolymorphicSerializer(): TypeSpec {
            val serializerCls = cls.serializerClass()
            val serializer = TypeSpec.objectBuilder(serializerCls)
                .addSuperinterface(Types.JniSerializer.parameterizedBy(cls))
                .addFunction(
                    buildPackFunction(cls)
                        .addCode(packCodePolymorphic(this))
                        .build()
                )
                .addFunction(
                    buildUnpackFunction(cls)
                        .addCode(unpackCodePolymorphic(this))
                        .build()
                )
                .build()
            return serializer
        }


        val files = serializables.mapNotNull {
            val spec = when (it) {
                is SerialClass.DataClass -> it.generateDataClassSerializer()
                is SerialClass.Polymorphic -> it.generatePolymorphicSerializer()
            }
            spec?.let { spec ->
                FileSpec.builder(it.cls.serializerClass())
                    .addType(spec)
                    .build()
            }
        }
        return files.toList()
    }

    private fun packCodePolymorphic(cls: SerialClass.Polymorphic): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("when (value)")
        cls.subclasses.forEachIndexed { i, dataClass ->
            val packProps = packCode(dataClass.properties)
            val pack = CodeBlock.builder().beginControlFlow("is %T -> ", dataClass.cls)
                .addStatement("// Write discriminator")
                .addStatement("buffer.writeInt(%L)", i)
                .addStatement("// Write props")
                .add(packProps)
                .endControlFlow()
                .build()
            builder.add(pack)
        }
        return builder.endControlFlow().build()
    }

    private fun unpackCodePolymorphic(cls: SerialClass.Polymorphic): CodeBlock {
        val builder = CodeBlock.builder()
        builder.addStatement("val d = buffer.readInt()")
        builder.beginControlFlow("when (d)")
        cls.subclasses.forEachIndexed { i, dataClass ->
            builder.beginControlFlow("%L -> ", i)
                .addStatement("// %L", dataClass.cls.simpleName)
                .add(unpackCode(dataClass))
                .endControlFlow()
        }
        builder.beginControlFlow("else ->")
            .addStatement("error(%S + d)", "Unknown discriminator for type ${cls.cls.simpleName}: ")
            .endControlFlow()
        return builder.endControlFlow().build()
    }

    private fun packCode(props: List<SerialProp>): CodeBlock {
        val builder = CodeBlock.builder()
        props.map {
            val serializer = try {
                included.serializer(it.type, it.overrideSerializer)
            } catch (e: NoSerializerException) {
                logger.error(e.message!!, it.declaration)
                throw e
            }
            if (serializer is IncludedSerializers.Serializer.Generic) registry.genericSerializers.add(serializer)
            val statement = serializer.writeCode(CodeBlock.of("buffer"), CodeBlock.of("value.%N", it.name))
            builder.add("\n%L\n", statement)
        }
        return builder.build()
    }

    private fun unpackCode(info: SerialClass.DataClass): CodeBlock {
        val builder = CodeBlock.builder()
        info.properties.forEach {
            val serializer = try {
                included.serializer(it.type, it.overrideSerializer)
            } catch (e: NoSerializerException) {
                logger.error(e.message!!, it.declaration)
                throw e
            }
            builder.add("\nval %N = %L\n", it.name, serializer.readCode(CodeBlock.of("buffer")))
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
                ParameterSpec("buffer", Types.IoBuffer)
            )
    }

    private fun buildUnpackFunction(type: TypeName): FunSpec.Builder {
        return FunSpec.builder("unpackFrom")
            .addModifiers(KModifier.OVERRIDE)
            .returns(type)
            .addParameter(
                ParameterSpec("buffer", Types.IoBuffer)
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
                    .addSuperinterface(Types.JniSerializer.parameterizedBy(type))
                    .addFunction(
                        FunSpec.builder("packTo")
                            .addParameter(ParameterSpec.builder("value", type).build())
                            .addParameter(ParameterSpec.builder("buffer", Types.IoBuffer).build())
                            .addModifiers(KModifier.OVERRIDE)
                            .addCode(
                                CodeBlock.of("%L", serializer.writeCode(CodeBlock.of("buffer"),
                                    CodeBlock.of("value")))
                            ).build()
                    )
                    .addFunction(
                        FunSpec.builder("unpackFrom")
                            .addParameter(ParameterSpec.builder("buffer", Types.IoBuffer).build())
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
        val typed = PropertySpec.builder(name.simpleName, Types.JniSerializer.parameterizedBy(generic))
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

    sealed class SerialClass {
        abstract val cls: ClassName
        data class DataClass(
            override val cls: ClassName,
            val properties: List<SerialProp>
        ): SerialClass()

        data class Polymorphic(
            override val cls: ClassName,
            val subclasses: List<DataClass>
        ): SerialClass()
    }

    data class SerialProp(
        val name: String,
        val type: TypeName,
        val declaration: KSNode,
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