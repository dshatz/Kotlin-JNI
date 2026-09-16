package com.dshatz.kni

import com.dshatz.kni.Registry.Platform.*
import com.dshatz.kni.annotations.TypeMarker
import com.dshatz.kni.kspfix.findAnnotation
import com.dshatz.kni.kspfix.getArgumentValueByName
import com.dshatz.kni.serialization.IncludedSerializers
import com.dshatz.kni.utils.PlatformContext
import com.dshatz.kni.utils.ProcessorContext
import com.dshatz.kni.utils.ResolverContext
import com.dshatz.kni.utils.SymContext
import com.dshatz.kni.utils.SymbolContext
import com.dshatz.kni.utils.TypeMappingContext
import com.dshatz.kni.utils.TypedCode
import com.dshatz.kni.utils.callFunction
import com.dshatz.kni.utils.capitalized
import com.dshatz.kni.utils.dereferenceTypeAlias
import com.dshatz.kni.utils.notNullable
import com.dshatz.kni.utils.nullSafeCall
import com.dshatz.kni.utils.returnType
import com.dshatz.kni.utils.safeQualifiedName
import com.dshatz.kni.utils.withSuffix
import com.google.devtools.ksp.getFunctionDeclarationsByName
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toTypeName

class TypeMapper(
    private val registry: Registry,
    private val logger: KSPLogger,
) {

    private val included = IncludedSerializers(registry, logger)

    private data class CacheKey(
        val platformContext: PlatformContext,
        val kotlinType: TypeName
    )

    private val typeCache = mutableMapOf<CacheKey, TypeInfo>()

    context(ctx: ResolverContext, p: PlatformContext)
    fun mapType(
        typeRef: KSTypeReference,
        saveType: Boolean = true
    ): TypeInfo {
        val type = typeRef.dereferenceTypeAlias()
        context(ctx, p, SymContext(typeRef)) {
            return mapType(type, saveType = saveType)
        }
    }

    context(ctx: ResolverContext, p: PlatformContext, symbolContext: SymbolContext)
    fun mapType(
        type: KSType,
        saveType: Boolean = true
    ): TypeInfo {
        val typeArguments = type.arguments.map { it.type!!.toTypeName() }
        return mapType(type.toTypeName(), typeArguments, saveType = saveType)
    }

    context(res: ResolverContext, p: PlatformContext, ctx: SymbolContext)
    fun mapType(
        kotlinType: TypeName,
        typeArgs: List<TypeName> = emptyList(),
        saveType: Boolean = true
    ): TypeInfo {
        val cacheKey = CacheKey(p, kotlinType)
        typeCache[cacheKey]?.let {
            logger.logging("Skipping mapType - already cached!")
            return it
        }
        val nonNull = kotlinType.copy(nullable = false)
        val nullable = kotlinType.isNullable
        val rawType = (nonNull as? ParameterizedTypeName)?.rawType ?: nonNull

        val jniField = Types.jniFields[nonNull] ?: "l"
        val mapped = if (nonNull in Types.jTypes) {
            // has an existing j type
            val jType = Types.jTypes[nonNull]!!
            if (nonNull in Types.toJTypes && jType in Types.toKTypes) {
                // We have converters to and from
                val (jvmType, nativeType) = if (nonNull in Types.boxedWhenNullable && kotlinType.isNullable) {
                    kotlinType.notNullable() to jType
                } else {
                    kotlinType to jType.copy(nullable = nullable)
                }
                TypeInfo.ConvertibleOnNative(
                    kotlinType = kotlinType,
                    jniType = JNIType(
                        chooseType(kotlinType, jvmType, nativeType),
                        jniField
                    ),
                    toJni = Types.toJTypes[nonNull]!!,
                    fromJni = Types.toKTypes[jType]!!
                )
            } else {
                // No converter, so it is a primitive - e.g. jfloat
                val (jvmType, nativeType) = if (nonNull in Types.boxedWhenNullable && kotlinType.isNullable) {
                    nonNull to jType.notNullable()
                } else {
                    kotlinType to jType
                }
                TypeInfo.Simple(
                    kotlinType = kotlinType,
                    jniType = JNIType(chooseType(kotlinType, jvmType, nativeType), jniField)
                )
            }
        } else if (rawType == Types.KArray) {
            (kotlinType as ParameterizedTypeName).typeArguments.first()
            TypeInfo.Array(
                innerType = mapType(
                    kotlinType = typeArgs.first(),
                    typeArgs = emptyList(),
                    saveType = saveType
                ),
                kotlinType,
                jniType = JNIType(chooseType(kotlinType, kotlinType, Types.JObjectArray).copy(nullable = nullable),"l")
            )
        } else if (rawType in registry.serializers || nonNull in registry.serializers) {
            // custom serializer defined
            val serializer = context(ctx.decl) { included.serializer(nonNull) }
            TypeInfo.Serializable(
                kotlinType = kotlinType,
                jniType = JNIType(
                    jniType = chooseType(kotlinType, Types.KByteArray, Types.JByteArray).copy(nullable = nullable),
                    jniField
                ),
                serializer = serializer,
            )
        } else if (registry.isCallback(nonNull)) {
            TypeInfo.callback(kotlinType as ClassName)
        } else if (nonNull in registry.nativeInstanceClasses) {
            val baseClass = registry.nativeInstances[nonNull]?.baseClass
            TypeInfo.nativeInstance(kotlinType as ClassName, baseClass)
        } else if (nonNull == Types.KByteBuffer) {
            TypeInfo.byteBuffer(kotlinType)
        } else if (nonNull == UNIT) {
            TypeInfo.Simple(
                kotlinType = kotlinType,
                jniType = JNIType(kotlinType, jniField)
            )
        } else if (nonNull in registry.jniAdapters) {
            val adapter = registry.jniAdapterTypes[nonNull]
            if (adapter != null) {
                val inner = context(SymContext(ctx.decl, forAdapter = true)) {
                    mapType(
                        kotlinType = adapter.inner,
                        saveType = false
                    )
                }
                TypeInfo.JniAdapter(kotlinType, inner, adapter.adapterCls)
            } else {
                /*
                 This source set does not define a JvmJniAdapter or NativeJniAdapter.
                 */
                logger.info("Wrapping $kotlinType as a TypeInfo.Simple in current sourceset.")
                TypeInfo.Simple(kotlinType, JNIType(kotlinType, "l"))
            }
        } else if (markerClass(kotlinType) != null) {
            val markers = asConvertibleOrNull(kotlinType)
            markers ?: TypeInfo.Simple(kotlinType, JNIType(kotlinType, "l"))
        } else {
            val typeStr = if (kotlinType is ParameterizedTypeName)
                "$kotlinType (raw: ${kotlinType.rawType})"
            else kotlinType.toString()

            if (ctx.forAdapter) {
//                logger.warn("Passing $kotlinType as Simple as no better alternative found.")
                TypeInfo.Simple(kotlinType, JNIType(kotlinType, "l"))
            } else {

                val error = """
                    Unknown type $typeStr - don't know how to pass to JNI.
                    
                    ===
                    
                    ${registry.serializersToString()}
                    
                    ===
                    
                    ${registry.nativeInstancesToString()}
                    
                    ===
                    
                    ${registry.jniAdaptersToString()}
                """.trimIndent()
                logger.error(error)
                error("JNI type mapping failed - see above for errors.")
            }

        }
        if (saveType) {
            registry.allTypes.add(mapped.notNullable())
        }
        typeCache[cacheKey] = mapped
        return mapped
    }

    context(ctx: ResolverContext)
    private fun asConvertibleOrNull(
        kotlinType: TypeName
    ): TypeInfo.Convertible? {
        val markerCls = markerClass(kotlinType)
        if (markerCls != null) {
            val packages = markerCls.findAnnotation<TypeMarker>()!!.getArgumentValueByName<List<String>>("packages")
            val pkg = packages?.firstOrNull()
            logger.info("Marker for $kotlinType package: $pkg")
            if (pkg != null) {
                val toJni = MemberName(pkg, "toJni")
                val fromJni = MemberName(pkg, "fromJni")
                val toJniF = ctx.resolver.getFunctionDeclarationsByName(toJni.canonicalName, true).firstOrNull()
                val fromJniF = ctx.resolver.getFunctionDeclarationsByName(fromJni.canonicalName, true).firstOrNull()
                if (toJniF != null && fromJniF != null) {
                    val jniType = toJniF.returnType!!.toTypeName()
                    return TypeInfo.Convertible(
                        kotlinType = kotlinType.notNullable(),
                        jniType = JNIType(jniType, "l"),
                        toJni = toJni,
                        fromJni = fromJni
                    )
                }
            }
        }
        return null
    }

    context(res: ResolverContext)
    private fun markerClass(
        kotlinType: TypeName
    ): KSClassDeclaration? {
        val name = "Marker${kotlinType.safeQualifiedName().replace('.', '_')}"
        val ksName = res.resolver.getKSNameFromString("kni.generated.adapters.$name")
        return res.resolver.getClassDeclarationByName(ksName)
    }

}

context(ctx: PlatformContext)
private fun chooseType(kotlinType: TypeName, jvmType: TypeName, nativeType: TypeName): TypeName {
    return when (ctx.platform) {
        COMMON -> kotlinType
        NATIVE -> nativeType
        JVM -> jvmType
    }
}

data class JNIType(
    val jniType: TypeName,
    val jniField: String
) {
    fun notNullable(): JNIType {
        return copy(jniType = jniType.notNullable())
    }
}

fun TypeInfo.needsIsNullParam(): Boolean {
    return kotlinType.needsIsNullParam()
}

fun TypeName.needsIsNullParam(): Boolean {
    return isNullable && notNullable() in Types.boxedWhenNullable
}

sealed class TypeInfo {
    abstract val kotlinType: TypeName
    abstract val jniType: JNIType

    open val commonKotlinType: TypeName by lazy { kotlinType }

    abstract fun packCode(unpackedCode: TypedCode): TypedCode
    abstract fun unpackCode(packedCode: TypedCode): TypedCode
    abstract fun packCodeJvm(unpackedCode: TypedCode): TypedCode
    abstract fun unpackCodeJvm(packedCode: TypedCode): TypedCode

    abstract fun describe(): String

    abstract fun notNullable(): TypeInfo

    companion object {
        val Unit = Simple(UNIT, JNIType(UNIT, "l"))

        context(_: PlatformContext)
        val PlatformString: TypeInfo get() =
            ConvertibleOnNative(
                Types.KString,
                JNIType(chooseType(Types.KString, Types.KString, Types.JString), "l"),
                toJni = Types.toJTypes[Types.KString]!!,
                fromJni = Types.toKTypes[Types.JString]!!
            )

        context(ctx: PlatformContext)
        fun byteBuffer(
            kotlinType: TypeName,
        ): ByteBuffer {
            return ByteBuffer(
                kotlinType = kotlinType,
                jniType = JNIType(
                    jniType = chooseType(kotlinType, Types.KNioBuffer, Types.JObject).copy(nullable = kotlinType.isNullable),
                    "l"
                )
            )
        }

        context(ctx: PlatformContext)
        fun nativeInstance(
            kotlinType: ClassName,
            baseType: ClassName? = null,
        ): NativeInstance {
            return NativeInstance(
                kotlinType = kotlinType,
                baseType = baseType,
                jniType = JNIType(chooseType(kotlinType, Types.KLong, Types.JLong).copy(nullable = kotlinType.isNullable), "j")
            )
        }

        context(ctx: PlatformContext)
        fun callback(
            kotlinType: ClassName,
            baseClass: ClassName? = null,
        ): Callback {
            return Callback(
                kotlinType = kotlinType,
                commonBaseClass = baseClass,
                jniType = JNIType(chooseType(
                    kotlinType,
                    baseClass ?: kotlinType,
                    Types.JObject.copy(nullable = kotlinType.isNullable)
                ), "l")
            )
        }
    }

    /**
     * jint, jfloat, jdouble, etc
     */
    data class Simple(
        override val kotlinType: TypeName,
        override val jniType: JNIType
    ): TypeInfo() {
        override fun packCode(unpackedCode: TypedCode): TypedCode {
            return unpackedCode.copy(type = jniType.jniType)
        }
        override fun unpackCode(packedCode: TypedCode): TypedCode {
            return packedCode.copy(type = kotlinType)
        }
        override fun packCodeJvm(unpackedCode: TypedCode): TypedCode = unpackedCode
        override fun unpackCodeJvm(packedCode: TypedCode): TypedCode = packedCode
        override fun describe(): String {
            return ""
        }

        override fun notNullable(): TypeInfo {
            return copy(kotlinType = kotlinType.notNullable(), jniType = jniType.notNullable())
        }
    }

    data class Array (
        val innerType: TypeInfo,
        override val kotlinType: TypeName = Types.KArray.parameterizedBy(innerType.kotlinType),
        override val jniType: JNIType
    ): TypeInfo() {

        override fun packCode(unpackedCode: TypedCode): TypedCode {
            return unpackedCode.callFunction(Types.Method.ToJoObjectArray, Types.JObjectArray) {
                named("env", CodeBlock.of("env"))
                lambdaParam("convert", receiverType = innerType.kotlinType) {
                    innerType.packCode(`this`).code
                }
            }
        }

        override fun unpackCode(packedCode: TypedCode): TypedCode {
            return packedCode.callFunction(Types.Method.ToKoObjectArray, kotlinType) {
                named("env", CodeBlock.of("env"))
                lambdaParam("convert", receiverType = Types.JObject) {
                    innerType.unpackCode(`this`).code
                }
            }
        }

        override fun packCodeJvm(unpackedCode: TypedCode): TypedCode {
            return unpackedCode
        }

        override fun unpackCodeJvm(packedCode: TypedCode): TypedCode {
            return packedCode
        }

        override fun describe(): String {
            return "Array of $innerType"
        }

        override fun notNullable(): TypeInfo {
            return copy(kotlinType = kotlinType.notNullable(), jniType = jniType.notNullable())
        }
    }

    data class ConvertibleOnNative(
        override val kotlinType: TypeName,
        override val jniType: JNIType,
        val toJni: MemberName,
        val fromJni: MemberName
    ): TypeInfo() {
        private val env = if (kotlinType.notNullable() in Types.conversionWithoutEnv) {
            CodeBlock.of("")
        } else CodeBlock.of("env")

        override fun packCode(unpackedCode: TypedCode): TypedCode {
            return unpackedCode.nullSafeCall(CodeBlock.of("%M(%L)", toJni, env).returnType(jniType))
        }

        override fun unpackCode(packedCode: TypedCode): TypedCode {
            return packedCode.nullSafeCall(
                CodeBlock.of("%M(%L)", fromJni, env).returnType(kotlinType)
            )
        }

        override fun packCodeJvm(unpackedCode: TypedCode): TypedCode = unpackedCode
        override fun unpackCodeJvm(packedCode: TypedCode): TypedCode = packedCode
        override fun describe(): String {
            return ""
        }

        override fun notNullable(): TypeInfo {
            return copy(kotlinType = kotlinType.notNullable(), jniType = jniType.notNullable())
        }
    }

    data class Convertible(
        override val kotlinType: TypeName,
        override val jniType: JNIType,
        val toJni: MemberName,
        val fromJni: MemberName,
    ): TypeInfo() {
        private val env = if (kotlinType.notNullable() in Types.conversionWithoutEnv) {
            CodeBlock.of("")
        } else CodeBlock.of("env")

        val aliasedImports = listOf(
            toJni to "toJniExternal",
            fromJni to "fromJniExternal"
        )
        private val toJniExternal = "toJniExternal"
        private val fromJniExternal = "fromJniExternal"

        override fun packCode(unpackedCode: TypedCode): TypedCode {
            return unpackedCode.nullSafeCall(CodeBlock.of("%L(%L)", toJniExternal, env).returnType(jniType))
        }
        override fun unpackCode(packedCode: TypedCode): TypedCode {
            return packedCode.nullSafeCall(CodeBlock.of("%L(%L)", fromJniExternal, env).returnType(kotlinType))
        }
        override fun packCodeJvm(unpackedCode: TypedCode): TypedCode {
            return unpackedCode.nullSafeCall(CodeBlock.of("%L()", toJniExternal).returnType(jniType))
        }
        override fun unpackCodeJvm(packedCode: TypedCode): TypedCode {
            return packedCode.nullSafeCall(CodeBlock.of("%L()", fromJniExternal).returnType(kotlinType))
        }
        override fun describe(): String {
            return ""
        }

        override fun notNullable(): TypeInfo {
            return copy(kotlinType = kotlinType.notNullable(), jniType = jniType.notNullable())
        }
    }

    data class JniAdapter(
        override val kotlinType: TypeName,
        val innerType: TypeInfo,
        private val adapterClassName: TypeName,
        override val jniType: JNIType = innerType.jniType
    ): TypeInfo() {
        override fun packCode(unpackedCode: TypedCode): TypedCode {
            val converted = unpackedCode.nullSafeCall(
                CodeBlock.builder()
                    .beginControlFlow("let")
                    .addStatement("%T.toJni(env, it)", adapterClassName)
                    .endControlFlow()
                    .build()
                    .returnType(jniType)
            )
            return innerType.packCode(converted)
        }

        override fun unpackCode(packedCode: TypedCode): TypedCode {
            val unpacked = innerType.unpackCode(packedCode)
            return unpacked.nullSafeCall(
                CodeBlock.builder()
                    .beginControlFlow("let")
                    .add("%T.fromJni(env, it)", adapterClassName)
                    .endControlFlow()
                    .build()
                    .returnType(kotlinType)
            )
        }

        override fun packCodeJvm(unpackedCode: TypedCode): TypedCode {
            val jniValue = unpackedCode.nullSafeCall(
                CodeBlock.of("let(%T::getJniValue)", adapterClassName).returnType(jniType)
            )
            return innerType.packCodeJvm(jniValue)
        }

        override fun unpackCodeJvm(packedCode: TypedCode): TypedCode {
            val unpacked = innerType.unpackCodeJvm(packedCode)
            return unpacked.nullSafeCall(
                CodeBlock.builder()
                    .add("let(%T::fromJniValue)", adapterClassName)
                    .build()
                    .returnType(kotlinType)
            )
        }

        override fun describe(): String {
            return "Adapter"
        }

        override fun notNullable(): TypeInfo {
            return copy(kotlinType = kotlinType.notNullable(), jniType = jniType.notNullable())
        }
    }


    data class Serializable(
        override val kotlinType: TypeName,
        override val jniType: JNIType,
        val serializer: IncludedSerializers.Serializer,
    ): TypeInfo() {
        override fun packCode(unpackedCode: TypedCode): TypedCode {
            return packCodeJvm(unpackedCode).nullSafeCall(
                CodeBlock.of(
                    "%M(env)",
                    Types.Method.ToJByteArray
                ).returnType(jniType)
            )
        }
        override fun unpackCode(packedCode: TypedCode): TypedCode {
            val byteArray = packedCode.nullSafeCall(
                CodeBlock.of(
                    "%M(env)",
                    Types.Method.ToKByteArray
                ).returnType(kotlinType)
            )
            return unpackCodeJvm(byteArray)
        }
        override fun packCodeJvm(unpackedCode: TypedCode): TypedCode {
            return serializer.writeCode(buffer = CodeBlock.of(""), unpackedCode)
        }
        override fun unpackCodeJvm(packedCode: TypedCode): TypedCode {
            return serializer.readCode(packedCode)
        }

        override fun describe(): String {
            return kotlinType.toString()
        }

        override fun notNullable(): TypeInfo {
            return copy(kotlinType = kotlinType.notNullable(), jniType = jniType.notNullable())
        }
    }

    data class ByteBuffer(
        override val kotlinType: TypeName = Types.KByteBuffer,
        override val jniType: JNIType
    ): TypeInfo() {
        override fun packCode(unpackedCode: TypedCode): TypedCode {
            // Create a jobject for common bytebuffer
            return unpackedCode.nullSafeCall(CodeBlock.of("%M(env)", Types.Method.ToJNioByteBuffer).returnType(Types.JObject))
        }

        override fun unpackCode(packedCode: TypedCode): TypedCode {
            return packedCode.nullSafeCall(CodeBlock.of("%M(env)", Types.Method.ToKDirectByteBuffer).returnType(kotlinType))
        }

        override fun packCodeJvm(unpackedCode: TypedCode): TypedCode {
            return unpackedCode.nullSafeCall(CodeBlock.of("jvmBuffer").returnType(Types.KNioBuffer))
        }

        override fun unpackCodeJvm(packedCode: TypedCode): TypedCode {
            return packedCode.nullSafeCall(
                CodeBlock.of("%M()", Types.Method.ToKByteBuffer).returnType(Types.KByteBuffer)
            )
        }

        override fun describe(): String {
            return ""
        }

        override fun notNullable(): TypeInfo {
            return copy(kotlinType = kotlinType.notNullable(), jniType = jniType.notNullable())
        }
    }

    data class NativeInstance(
        override val kotlinType: ClassName,
        private val baseType: ClassName? = null,
        override val jniType: JNIType
    ): TypeInfo() {
        override val commonKotlinType: ClassName = baseType ?: kotlinType

        override fun packCode(unpackedCode: TypedCode): TypedCode {
            assert(unpackedCode.type.notNullable() == kotlinType)
            return unpackedCode.nullSafeCall(
                CodeBlock.of("%M()", Types.Method.asStableRefLongPointer)
                    .returnType(Types.KLong)
            )
        }

        override fun unpackCode(packedCode: TypedCode): TypedCode {
            assert(packedCode.type.notNullable() == Types.KLong)
            return packedCode.nullSafeCall(
                CodeBlock.of("%M<%T>()", Types.Method.valueFromStableRefPointer, commonKotlinType)
                    .returnType(kotlinType)
            )
        }

        override fun packCodeJvm(unpackedCode: TypedCode): TypedCode {
            val asLong = MemberName(commonKotlinType.packageName, "asLong")
            return unpackedCode.nullSafeCall(CodeBlock.of("%M()", asLong).returnType(jniType))
        }

        override fun unpackCodeJvm(packedCode: TypedCode): TypedCode {
            val member = MemberName(kotlinType.packageName, "as${kotlinType.simpleName}")
            return packedCode.nullSafeCall(
                CodeBlock.of("%M()", member)
                    .returnType(kotlinType)
            )
        }

        override fun describe(): String {
            return "jobject instance"
        }

        override fun notNullable(): TypeInfo {
            return copy(kotlinType = kotlinType.notNullable(), jniType = jniType.notNullable())
        }
    }

    data class Callback(
        override val kotlinType: ClassName,
        private val commonBaseClass: ClassName? = null,
        override val jniType: JNIType
    ): TypeInfo() {
        override val commonKotlinType: ClassName = commonBaseClass ?: kotlinType

        private val asNative = kotlinType.let {
            MemberName(it.packageName, "asNative${it.simpleName.capitalized()}")
        }

        override fun packCode(unpackedCode: TypedCode): TypedCode {
            return unpackedCode.nullSafeCall(
                CodeBlock.builder()
                    .beginControlFlow("let")
                    .addStatement("(it as %T).ref", kotlinType.withSuffix("_Native"))
                    .endControlFlow()
                    .build()
                    .returnType(jniType)
            )
        }

        override fun unpackCode(packedCode: TypedCode): TypedCode {
            return packedCode.nullSafeCall(
                CodeBlock.of("%M(env)", asNative).returnType(kotlinType)
            )
        }

        override fun packCodeJvm(unpackedCode: TypedCode): TypedCode {
            return unpackedCode
        }

        override fun unpackCodeJvm(packedCode: TypedCode): TypedCode {
            return packedCode
        }

        override fun describe(): String {
            return "@JniCallback annotated jobject"
        }

        override fun notNullable(): TypeInfo {
            return copy(kotlinType = kotlinType.notNullable(), jniType = jniType.notNullable())
        }
    }
}