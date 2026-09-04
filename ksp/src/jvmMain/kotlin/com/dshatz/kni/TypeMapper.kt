package com.dshatz.kni

import com.dshatz.kni.serialization.IncludedSerializers
import com.dshatz.kni.utils.TypedCode
import com.dshatz.kni.utils.addStatement
import com.dshatz.kni.utils.callFunction
import com.dshatz.kni.utils.capitalized
import com.dshatz.kni.utils.dereferenceTypeAlias
import com.dshatz.kni.utils.notNullable
import com.dshatz.kni.utils.nullSafeCall
import com.dshatz.kni.utils.returnType
import com.dshatz.kni.utils.withSuffix
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toTypeName

class TypeMapper(
    private val registry: Registry,
    private val logger: KSPLogger
) {

    private val included = IncludedSerializers(registry, logger)

    fun mapType(
        typeRef: KSTypeReference
    ): TypeInfo {
        val type = typeRef.dereferenceTypeAlias()
        context(typeRef) {
            return mapType(type)
        }
    }

    context(decl: KSNode)
    fun mapType(
        type: KSType,
    ): TypeInfo {
        val typeArguments = type.arguments.map { it.type!!.toTypeName() }
        return mapType(type.toTypeName(), typeArguments)
    }

    context(decl: KSNode)
    fun mapType(
        kotlinType: TypeName,
        typeArgs: List<TypeName> = emptyList(),
        allowSelf: Boolean = false,
    ): TypeInfo {
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
                TypeInfo.Convertible(
                    kotlinType = kotlinType,
                    jniType = JNIType(
                        jvmType,
                        nativeType,
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
                    jniType = JNIType(jvmType = jvmType, nativeType, jniField)
                )
            }
        } else if (rawType == Types.KArray) {
            (kotlinType as ParameterizedTypeName).typeArguments.first()
            TypeInfo.Array(
                innerType = mapType(typeArgs.first(), emptyList()),
                kotlinType
            )
        } else if (rawType in registry.serializers || nonNull in registry.serializers) {
            // custom serializer defined
            val serializer = included.serializer(nonNull)
            TypeInfo.Serializable(
                kotlinType = kotlinType,
                jniType = JNIType(
                    Types.KByteArray.copy(nullable = nullable),
                    Types.JByteArray.copy(nullable = nullable),
                    jniField
                ),
                serializer = serializer,
            )
        } else if (registry.isCallback(nonNull)) {
            TypeInfo.Callback(kotlinType as ClassName)
        } else if (nonNull in registry.nativeInstanceClasses) {
            val baseClass = registry.nativeInstances[nonNull]?.baseClass
            TypeInfo.NativeInstance(kotlinType as ClassName, baseClass)
        } else if (nonNull == Types.KByteBuffer) {
            TypeInfo.ByteBuffer(nullable = nullable)
        } else if (nonNull == UNIT) {
            TypeInfo.Simple(
                kotlinType = kotlinType,
                jniType = JNIType(kotlinType, kotlinType, jniField)
            )
        } else if (nonNull in registry.jniAdapters) {
            val adapter = registry.jniAdapterTypes[nonNull]
            if (adapter != null) {
                val inner = mapType(adapter.inner, allowSelf = true)
                TypeInfo.JniAdapter(kotlinType, inner, adapter.adapterCls)
            } else {
                /*
                 This source set does not define a JvmJniAdapter or NativeJniAdapter.
                 */
                logger.info("Wrapping $kotlinType as a TypeInfo.Simple in current sourceset.")
                TypeInfo.Simple(kotlinType, JNIType(kotlinType, kotlinType, "l"))
            }
        } else {
            if (allowSelf) {
                // JniAdapter class, pass class as it appears in kotlin code if not found
                TypeInfo.Simple(kotlinType, JNIType(kotlinType, kotlinType, "l"))
            } else {
                val typeStr = if (kotlinType is ParameterizedTypeName)
                    "$kotlinType (raw: ${kotlinType.rawType})"
                else kotlinType.toString()

                val error = """
                Unknown type $typeStr - don't know how to pass to JNI.
                
                ===
                
                ${registry.serializersToString()}
                
                ===
                
                ${registry.nativeInstancesToString()}
                
                ===
                
                ${registry.jniAdaptersToString()}
            """
                logger.error(error)
                error("JNI type mapping failed - see above for errors.")
            }
        }
        registry.allTypes.add(mapped.notNullable())
        return mapped
    }
}


data class JNIType(
    val jvmType: TypeName,
    val nativeType: TypeName,
    val jniField: String
) {
    fun notNullable(): JNIType {
        return copy(jvmType = jvmType.notNullable(), nativeType = nativeType.notNullable())
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
        val Unit = Simple(UNIT, JNIType(UNIT, UNIT, "l"))
        val STRING = Convertible(
            Types.KString,
            JNIType(Types.KString, Types.JString, "l"),
            toJni = Types.toJTypes[Types.KString]!!,
            fromJni = Types.toKTypes[Types.JString]!!
        )
    }

    /**
     * jint, jfloat, jdouble, etc
     */
    data class Simple(
        override val kotlinType: TypeName,
        override val jniType: JNIType
    ): TypeInfo() {
        override fun packCode(unpackedCode: TypedCode): TypedCode {
            return unpackedCode.copy(type = jniType.nativeType)
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

    data class Array(
        val innerType: TypeInfo,
        override val kotlinType: TypeName = Types.KArray.parameterizedBy(innerType.kotlinType),
    ): TypeInfo() {
        override val jniType: JNIType = JNIType(kotlinType, Types.JObjectArray, "l")

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
            return copy(kotlinType = kotlinType.notNullable())
        }
    }
    /**
     * jboolean, jchar, jstring, j*array
     */
    data class Convertible(
        override val kotlinType: TypeName,
        override val jniType: JNIType,
        val toJni: MemberName,
        val fromJni: MemberName
    ): TypeInfo() {
        private val env = if (kotlinType.notNullable() in Types.conversionWithoutEnv) {
            CodeBlock.of("")
        } else CodeBlock.of("env")

        override fun packCode(unpackedCode: TypedCode): TypedCode {
            return unpackedCode.nullSafeCall(CodeBlock.of("%M(%L)", toJni, env).returnType(jniType.nativeType))
        }
        override fun unpackCode(packedCode: TypedCode): TypedCode {
            return packedCode.nullSafeCall(CodeBlock.of("%M(%L)", fromJni, env).returnType(kotlinType))
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
                    .returnType(jniType.nativeType)
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
                CodeBlock.of("let(%T::getJniValue)", adapterClassName).returnType(jniType.jvmType)
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
            return "Wrapper"
        }

        override fun notNullable(): TypeInfo {
            return copy(kotlinType = kotlinType.notNullable(), jniType = jniType.notNullable())
        }

    }

    data class Serializable(
        override val kotlinType: TypeName,
        override val jniType: JNIType = JNIType(
            Types.KByteArray.copy(nullable = kotlinType.isNullable),
            Types.JByteArray.copy(nullable = kotlinType.isNullable),
            "l"
        ),
        val serializer: IncludedSerializers.Serializer,
    ): TypeInfo() {
        override fun packCode(unpackedCode: TypedCode): TypedCode {
            return packCodeJvm(unpackedCode).nullSafeCall(
                CodeBlock.of(
                    "%M(env)",
                    Types.Method.ToJByteArray
                ).returnType(jniType.nativeType)
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
        override val jniType: JNIType = JNIType(
            Types.KNioBuffer.copy(nullable = kotlinType.isNullable),
            Types.JObject.copy(nullable = kotlinType.isNullable),
            "l"
        ),
    ): TypeInfo() {
        constructor(nullable: Boolean): this(
            kotlinType = Types.KByteBuffer.copy(nullable = nullable),
        )
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
        private val baseType: ClassName? = null
    ): TypeInfo() {
        override val commonKotlinType: ClassName = baseType ?: kotlinType
        override val jniType: JNIType = JNIType(
            LONG.copy(nullable = kotlinType.isNullable),
            Types.JLong.copy(nullable = kotlinType.isNullable),
            "j"
        )

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
            return unpackedCode.nullSafeCall(CodeBlock.of("%M()", asLong).returnType(jniType.jvmType))
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
            return copy(kotlinType = kotlinType.notNullable() as ClassName)
        }
    }

    data class Callback(
        override val kotlinType: ClassName,
        private val commonBaseClass: ClassName? = null
    ): TypeInfo() {
        override val commonKotlinType: ClassName = commonBaseClass ?: kotlinType
        override val jniType: JNIType = JNIType(
            jvmType = commonKotlinType,
            nativeType = Types.JObject.copy(nullable = kotlinType.isNullable),
            "l"
        )

        private val asNative = (kotlinType as ClassName).let {
            MemberName(it.packageName, "asNative${it.simpleName.capitalized()}")
        }

        override fun packCode(unpackedCode: TypedCode): TypedCode {
            return unpackedCode.nullSafeCall(
                CodeBlock.builder()
                    .beginControlFlow("let")
                    .addStatement("(it as %T).ref", (kotlinType as ClassName).withSuffix("_Native"))
                    .endControlFlow()
                    .build()
                    .returnType(jniType.nativeType)
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
            return copy(kotlinType = kotlinType.notNullable())
        }
    }
}