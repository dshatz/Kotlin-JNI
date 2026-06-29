package com.dshatz.kni

import com.dshatz.kni.callable.getNativeImplClass
import com.dshatz.kni.serialization.IncludedSerializers
import com.dshatz.kni.utils.TypedCode
import com.dshatz.kni.utils.checkNotNull
import com.dshatz.kni.utils.dereferenceTypeAlias
import com.dshatz.kni.utils.notNullable
import com.dshatz.kni.utils.nullSafeCall
import com.dshatz.kni.utils.returnType
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
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
        type: KSType
    ): TypeInfo {
        return mapType(type.toTypeName())
    }

    context(decl: KSNode)
    fun mapType(
        kotlinType: TypeName,
    ): TypeInfo {
        val nonNull = kotlinType.copy(nullable = false)
        val nullable = kotlinType.isNullable
        val rawType = (nonNull as? ParameterizedTypeName)?.rawType ?: nonNull

        val jniField = Types.jniFields[nonNull] ?: "l"
        return if (nonNull in Types.jTypes) {
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
        } else if (rawType in registry.serializers) {
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
        } else if (nonNull in registry.callbacks) {
            TypeInfo.Callback(kotlinType)
        } else if (nonNull in registry.nativeInstances) {
            TypeInfo.NativeInstance(kotlinType)
        } else if (nonNull == Types.KByteBuffer) {
            TypeInfo.ByteBuffer(nullable = nullable)
        } else if (nonNull == UNIT) {
            TypeInfo.Simple(
                kotlinType = kotlinType,
                jniType = JNIType(kotlinType, kotlinType, jniField)
            )
        } else {
            logger.error("Unknown type: $kotlinType, don't know how to pass to JNI. ${registry.serializersToString()}", decl)
            error("JNI type mapping failed - see above for errors.")
            /*TypeInfo.Simple(
                kotlinType = kotlinType,
                jniType = JNIType(kotlinType, kotlinType)
            )*/
        }
    }
}


data class JNIType(
    val jvmType: TypeName,
    val nativeType: TypeName,
    val jniField: String
)

fun TypeInfo.needsIsNullParam(): Boolean {
    return kotlinType.needsIsNullParam()
}

fun TypeName.needsIsNullParam(): Boolean {
    return isNullable && notNullable() in Types.boxedWhenNullable
}

sealed class TypeInfo {
    abstract val kotlinType: TypeName
    abstract val jniType: JNIType

    abstract fun packCode(unpackedCode: TypedCode): TypedCode
    abstract fun unpackCode(packedCode: TypedCode): TypedCode
    abstract fun packCodeJvm(unpackedCode: TypedCode): TypedCode
    abstract fun unpackCodeJvm(packedCode: TypedCode): TypedCode

    abstract fun describe(): String

    val nullCheck get() = if (kotlinType.isNullable && kotlinType.notNullable() !in Types.boxedWhenNullable) {
        CodeBlock.of("?.")
    } else CodeBlock.of(".")

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
            return CodeBlock.of(
                "%L.%M(env)",
                packCodeJvm(unpackedCode).code,
                Types.Method.ToJByteArray
            ).returnType(jniType.nativeType)
        }
        override fun unpackCode(packedCode: TypedCode): TypedCode {
            val deserializeCode = unpackCodeJvm(CodeBlock.of(
                "%L.%M(env)",
                packedCode.code,
                Types.Method.ToKByteArray
            ).returnType(kotlinType))
            return packedCode.checkNotNull { deserializeCode }
        }
        override fun packCodeJvm(unpackedCode: TypedCode): TypedCode {
            val serializeCode = serializer.writeCode(buffer = CodeBlock.of(""), unpackedCode.code)
                .returnType(Types.KByteArray)
            return unpackedCode.checkNotNull { serializeCode }
        }
        override fun unpackCodeJvm(packedCode: TypedCode): TypedCode {
            return packedCode.checkNotNull {
                serializer.readCode(it).returnType(kotlinType)
            }
        }

        override fun describe(): String {
            return kotlinType.toString()
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
            return packedCode.checkNotNull { CodeBlock.of("%T(%L)", Types.KByteBuffer, it).returnType(Types.KByteBuffer) }
        }

        override fun describe(): String {
            return ""
        }
    }

    data class NativeInstance(
        override val kotlinType: TypeName
    ): TypeInfo() {
        override val jniType: JNIType = JNIType(
            LONG.copy(nullable = kotlinType.isNullable),
            Types.JLong.copy(nullable = kotlinType.isNullable),
            "j"
        )

        override fun packCode(unpackedCode: TypedCode): TypedCode {
            assert(unpackedCode.type.notNullable() == kotlinType)
            return unpackedCode.nullSafeCall(
                CodeBlock.of("%M()", Types.Method.AsLongPointer)
                    .returnType(Types.KLong)
            )
        }

        override fun unpackCode(packedCode: TypedCode): TypedCode {
            assert(packedCode.type.notNullable() == Types.KLong)
            return packedCode.nullSafeCall(
                CodeBlock.of("%M<%T>()", Types.Method.FromLongPointer, kotlinType)
                    .returnType(kotlinType)
            )
        }

        override fun packCodeJvm(unpackedCode: TypedCode): TypedCode {
            return unpackedCode
        }

        override fun unpackCodeJvm(packedCode: TypedCode): TypedCode {
            return packedCode
        }

        override fun describe(): String {
            return "jobject instance"
        }
    }

    data class Callback(
        override val kotlinType: TypeName,
    ): TypeInfo() {
        override val jniType: JNIType = JNIType(
            jvmType = kotlinType,
            nativeType = Types.JObject.copy(nullable = kotlinType.isNullable),
            "l"
        )

        override fun packCode(unpackedCode: TypedCode): TypedCode {
            error("Passing @Callback objects is only allowed in JVM->Native direction, not back.")
        }

        override fun unpackCode(packedCode: TypedCode): TypedCode {
            return packedCode.checkNotNull {
                CodeBlock.of("%T(env, %L)", kotlinType.getNativeImplClass(), it)
                    .returnType(kotlinType)
            }
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
    }
}