package com.dshatz.kni.serialization

import com.dshatz.kni.Registry
import com.dshatz.kni.Registry.Platform
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.Types
import com.dshatz.kni.annotations.JniSerializable
import com.dshatz.kni.annotations.JniSerializerFor
import com.dshatz.kni.kspfix.findAnnotation
import com.dshatz.kni.kspfix.getClassArgument
import com.dshatz.kni.model.KSDefinedSerializer
import com.dshatz.kni.utils.dereferenceTypeAlias
import com.dshatz.kni.utils.originatesFrom
import com.dshatz.kni.utils.returnType
import com.dshatz.kni.utils.withSuffix
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
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
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class SerializerProcessor(
    private val registry: Registry,
    private val logger: KSPLogger
) {

    private val included = IncludedSerializers(registry, logger)

    fun findSerializables(
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
                d.classKind == ClassKind.CLASS
            }
            .filter { d ->
                (Modifier.DATA in d.modifiers || Modifier.VALUE in d.modifiers).also {
                    if (!it) logger.warn("Ignoring @JniSerializable on a class: must have data or value modifiers.", d)
                }
            }
            .map(::collectSerialProps)

        val enums = declarations.filter { d ->
            d.classKind == ClassKind.ENUM_CLASS
        }.map {
            SerialClass.EnumClass(it.toClassName())
        }

        return dataClasses + sealed + enums
    }

    private fun KSPropertyDeclaration.getOverrideSerializer(): ClassName? {
        val annotation = findAnnotation<JniSerializable>()
        val paramValue = annotation?.getClassArgument("with")
        return paramValue
    }

    /**
     * Find all defined serializers, annotated with [JniSerializerFor] and save to registry as [KSDefinedSerializer].
     */
    fun collectDefinedSerializers(resolver: Resolver) {
        val serializers = resolver.getSymbolsWithAnnotation(JniSerializerFor::class.java.name)
            .filterIsInstance<KSClassDeclaration>()
        logger.info("Found ${serializers.toList().size} @JniSerializerFor declarations.")
        val defined = serializers.mapNotNull {
            if (it.classKind != ClassKind.OBJECT && it.classKind != ClassKind.CLASS) {
                logger.error("@JniSerializerFor must be applied to a class/object.", it)
                null
            } else {
                val serializable = it.findAnnotation<JniSerializerFor>()
                    ?.getClassArgument("target") ?: run {
                        logger.error("Could not read @JniSerializerFor annotation", it)
                        error("Could not read target argument")
                    }
                serializable to KSDefinedSerializer(
                    typeName = serializable,
                    serializer = it.toClassName()
                )
            }
        }.toMap()
        registry.serializers.putAll(defined)
    }

    fun collectGenericSerializers() {
        fun Sequence<TypeInfo>.requiredSerializers(): Sequence<IncludedSerializers.Serializer.Generic> {
            return filterIsInstance<TypeInfo.Serializable>()
                .map { it.serializer }
                .filterIsInstance<IncludedSerializers.Serializer.Generic>()
        }

        val usedInCalls = registry.jniCalls.asSequence().flatMap {
            it.parameters.map { it.typeInfo } + it.returnType
        }.requiredSerializers()

        val usedInCallbacks = registry.callbacks.values.asSequence().flatMap {
            it.funs.flatMap { f ->
                f.parameters.map { it.typeInfo } + f.returnType
            }
        }.requiredSerializers()

        fun SerialClass.collectSerializers(): List<IncludedSerializers.Serializer> {
            return when (this) {
                is SerialClass.DataClass -> properties.map {
                    context(it.declaration) {
                        included.serializer(it.type)
                    }
                }
                is SerialClass.EnumClass -> emptyList()
                is SerialClass.Polymorphic -> subclasses.flatMap(SerialClass.DataClass::collectSerializers)
            }
        }

        val usedInSerializers = registry.generatedSerializers.asSequence().flatMap {
            it.collectSerializers()
        }.filterIsInstance<IncludedSerializers.Serializer.Generic>()
        val serializersToGenerate = (
                usedInCalls
                        + usedInCallbacks
                        + usedInSerializers)
            .distinct()

        registry.genericSerializers.addAll(serializersToGenerate)
        val saved = serializersToGenerate.associate {
            it.type to KSDefinedSerializer(it.type,it.serializerClass)
        }
        registry.serializers.putAll(saved)
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
                    .superclass(Types.JniSerializer.parameterizedBy(cls))
                    .addSuperclassConstructorParameter("%S", cls.canonicalName)
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
                .superclass(Types.JniSerializer.parameterizedBy(cls))
                .addSuperclassConstructorParameter("%S", cls.canonicalName)
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

        fun SerialClass.EnumClass.generateEnumSerializer(): TypeSpec {
            val serializerCls = cls.serializerClass()
            val serializer = TypeSpec.objectBuilder(serializerCls)
                .superclass(Types.JniSerializer.parameterizedBy(cls))
                .addSuperclassConstructorParameter("%S", cls.canonicalName)
                .addFunction(
                    buildPackFunction(cls)
                        .addStatement("buffer.writeInt(value.ordinal)")
                        .build()
                )
                .addFunction(
                    buildUnpackFunction(cls)
                        .addStatement("return %T.entries[buffer.readInt()]", cls)
                        .build()
                ).build()
            return serializer
        }


        val files = serializables.mapNotNull {
            val spec = when (it) {
                is SerialClass.DataClass -> it.generateDataClassSerializer()
                is SerialClass.Polymorphic -> it.generatePolymorphicSerializer()
                is SerialClass.EnumClass -> it.generateEnumSerializer()
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
                context(it.declaration) {
                    included.serializer(it.type, it.overrideSerializer)
                }
            } catch (e: NoSerializerException) {
                logger.error(e.message!!, it.declaration)
                throw e
            }
            if (serializer is IncludedSerializers.Serializer.Generic) registry.genericSerializers.add(serializer)
            val paramValue = CodeBlock.of("value.%N", it.name).returnType(it.type)
            val statement = serializer.writeCode(
                CodeBlock.of("buffer"),
                paramValue
            ).code
            builder.add("\n%L\n", statement)
        }
        return builder.build()
    }

    private fun unpackCode(info: SerialClass.DataClass): CodeBlock {
        val builder = CodeBlock.builder()
        info.properties.forEach {
            val serializer = try {
                context(it.declaration) {
                    included.serializer(it.type, it.overrideSerializer)
                }
            } catch (e: NoSerializerException) {
                logger.error(e.message!!, it.declaration)
                throw e
            }
            builder.add("\nval %N = %L\n", it.name, serializer.readCode(CodeBlock.of("buffer").returnType(Types.IoBuffer)).code)
        }
        return builder.addStatement("return %T(%L)", info.cls, info.properties.joinToString { it.name }).build()
    }

    private fun buildPackFunction(type: TypeName): FunSpec.Builder {
        return FunSpec.builder("packToBuffer")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                ParameterSpec("value", type)
            )
            .addParameter(
                ParameterSpec("buffer", Types.IoBuffer)
            )
    }

    private fun buildUnpackFunction(type: TypeName): FunSpec.Builder {
        return FunSpec.builder("unpackFromBuffer")
            .addModifiers(KModifier.OVERRIDE)
            .returns(type)
            .addParameter(
                ParameterSpec("buffer", Types.IoBuffer)
            )
    }

    fun generateGeneric(serializer: IncludedSerializers.Serializer.Generic): GeneratedGenericSerializer {
        val typeParams = serializer.type.typeArguments.zip(serializer.argumentSerializer)
        val argSerializers = typeParams.mapNotNull { (type, serializer) ->
            if (serializer !is IncludedSerializers.Serializer.StaticObject) {
                val name = "SerializerFor_${(type as ClassName).simpleName}"
                val cls = ClassName(pkg, name)
                registry.serializers[type] = KSDefinedSerializer(type, cls)
                TypeSpec.objectBuilder(cls)
                    .superclass(Types.JniSerializer.parameterizedBy(type))
                    .addSuperclassConstructorParameter(CodeBlock.of("%S", type.canonicalName))
                    .addFunction(
                        FunSpec.builder("packToBuffer")
                            .addParameter(ParameterSpec.builder("value", type).build())
                            .addParameter(ParameterSpec.builder("buffer", Types.IoBuffer).build())
                            .addModifiers(KModifier.OVERRIDE)
                            .addCode(
                                serializer.writeCode(CodeBlock.of("buffer"),
                                CodeBlock.of("value").returnType(type)
                                ).code
                            ).build()
                    )
                    .addFunction(
                        FunSpec.builder("unpackFromBuffer")
                            .addParameter(ParameterSpec.builder("buffer", Types.IoBuffer).build())
                            .returns(type)
                            .addModifiers(KModifier.OVERRIDE)
                            .addCode(
                                CodeBlock.of("return %L", serializer.readCode(CodeBlock.of("buffer").returnType(Types.IoBuffer)).code)
                            ).build()
                    )
                    .build()
            } else null
        }

        val generic = serializer.type
        val serializerClass = serializer.serializerClass
        val typed = PropertySpec.builder(serializerClass.simpleName, Types.JniSerializer.parameterizedBy(generic))
            .initializer(CodeBlock.of(
                "%T(%L)",
                (serializer.rawSerializer as IncludedSerializers.Serializer.StaticObject).serializer,
                registry.serializers[generic.typeArguments.first()]?.serializer ?: error("Could not find serializer for type param ${generic.typeArguments.first()}. Existing: ${registry.serializersToString()}")
            ))
            .build()
        return GeneratedGenericSerializer(
            argSerializers,
            typed
        )
    }

    data class GeneratedGenericSerializer(
        val arg: List<TypeSpec>,
        val serializer: PropertySpec
    )

    fun generateGenericSerializers(): FileSpec? {
        if (registry.genericSerializers.isEmpty()) return null
        return FileSpec.builder(ClassName(pkg, "genericSerializers")).apply {
            registry.genericSerializers.map {
                generateGeneric(it)
            }.forEach {
                addProperty(it.serializer)
                addTypes(it.arg)
            }
        }.build()
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

        data class EnumClass(
            override val cls: ClassName
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
    return withSuffix("_Serializer_generated")
}