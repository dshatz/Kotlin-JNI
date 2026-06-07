package com.dshatz.kni

import com.dshatz.kni.callable.getNativeImplClass
import com.dshatz.kni.serialization.IncludedSerializers
import com.dshatz.kni.utils.dereferenceTypeAlias
import com.google.devtools.ksp.processing.KSPLogger
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
    logger: KSPLogger
) {

    private val included = IncludedSerializers(registry, logger)

    fun mapType(
        typeRef: KSTypeReference
    ): TypeInfo {
        val type = typeRef.dereferenceTypeAlias()
        return mapType(type)
    }

    fun mapType(
        type: KSType
    ): TypeInfo {
        return mapType(type.toTypeName())
    }

    fun mapType(
        kotlinType: TypeName,
    ): TypeInfo {
        val rawType = (kotlinType as? ParameterizedTypeName)?.rawType ?: kotlinType

        val jniType = TypeMatcher.jniFields[kotlinType] ?: "l"
        return if (kotlinType in TypeMatcher.jTypes) {
            // has an existing j type
            val jType = TypeMatcher.jTypes[kotlinType]!!
            if (kotlinType in TypeMatcher.toJTypes && jType in TypeMatcher.toKTypes) {
                // We have converters to and from
                TypeInfo.Convertible(
                    kotlinType = kotlinType,
                    jniType = JNIType(kotlinType, jType, jniType),
                    toJni = TypeMatcher.toJTypes[kotlinType]!!,
                    fromJni = TypeMatcher.toKTypes[jType]!!
                )
            } else {
                // No converter, so it is a primitive - e.g. jfloat
                TypeInfo.Simple(
                    kotlinType = kotlinType,
                    jniType = JNIType(jvmType = kotlinType, jType, jniType)
                )
            }
        } else if (rawType in registry.serializers) {
            // custom serializer defined
            val serializer = included.serializer(kotlinType)
//            val serializer = registry.serializers[rawType]!!
            /*val typeParams = (kotlinType as? ParameterizedTypeName)?.typeArguments?.map {
                registry.serializers[it] ?: error("No serializer defined for $it in $serializer")
            }.orEmpty()*/
            TypeInfo.Serializable(
                kotlinType = kotlinType,
                jniType = JNIType(TypeMatcher.KByteArray, TypeMatcher.JByteArray, jniType),
                serializer = serializer,
            )
        } else if (kotlinType in registry.callbacks) {
            TypeInfo.Callback(kotlinType)
        } else if (kotlinType in registry.nativeInstances) {
            TypeInfo.NativeInstance(kotlinType)
        } else if (kotlinType == TypeMatcher.KByteBuffer) {
            TypeInfo.ByteBuffer()
        } else if (kotlinType == UNIT) {
            TypeInfo.Simple(
                kotlinType = kotlinType,
                jniType = JNIType(kotlinType, kotlinType, jniType)
            )
        } else {
            error("Unknown type: $kotlinType, don't know how to pass to JNI. ${registry.serializersToString()}")
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

sealed class TypeInfo {
    abstract val kotlinType: TypeName
    abstract val jniType: JNIType

    abstract fun packCode(unpackedCode: CodeBlock): CodeBlock
    abstract fun unpackCode(packedCode: CodeBlock): CodeBlock
    abstract fun packCodeJvm(unpackedCode: CodeBlock): CodeBlock
    abstract fun unpackCodeJvm(packedCode: CodeBlock): CodeBlock

    abstract fun describe(): String

    data class Simple(
        override val kotlinType: TypeName,
        override val jniType: JNIType
    ): TypeInfo() {
        override fun packCode(unpackedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("%L", unpackedCode)
        }
        override fun unpackCode(packedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("%L", packedCode)
        }
        override fun packCodeJvm(unpackedCode: CodeBlock): CodeBlock = unpackedCode
        override fun unpackCodeJvm(packedCode: CodeBlock): CodeBlock = packedCode
        override fun describe(): String {
            return ""
        }
    }

    data class Convertible(
        override val kotlinType: TypeName,
        override val jniType: JNIType,
        val toJni: MemberName,
        val fromJni: MemberName
    ): TypeInfo() {
        private val env = if (kotlinType in TypeMatcher.conversionWithoutEnv) {
            CodeBlock.of("")
        } else CodeBlock.of("env")

        override fun packCode(unpackedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("(%L).%M(%L)!!", unpackedCode, toJni, env)
        }
        override fun unpackCode(packedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("(%L).%M(%L)!!", packedCode, fromJni, env)
        }
        override fun packCodeJvm(unpackedCode: CodeBlock): CodeBlock = unpackedCode
        override fun unpackCodeJvm(packedCode: CodeBlock): CodeBlock = packedCode
        override fun describe(): String {
            return ""
        }
    }

    data class Serializable(
        override val kotlinType: TypeName,
        override val jniType: JNIType = JNIType(TypeMatcher.KByteArray, TypeMatcher.JByteArray, "l"),
        val serializer: IncludedSerializers.Serializer,
    ): TypeInfo() {
        override fun packCode(unpackedCode: CodeBlock): CodeBlock {
            return CodeBlock.of(
                "%L.%M(env)!!",
                packCodeJvm(unpackedCode),
                TypeMatcher.Method.ToJByteArray
            )
        }
        override fun unpackCode(packedCode: CodeBlock): CodeBlock {
            return unpackCodeJvm(CodeBlock.of(
                "%L.%M(env)!!",
                packedCode,
                TypeMatcher.Method.ToKByteArray
            ))
        }
        override fun packCodeJvm(unpackedCode: CodeBlock): CodeBlock {
            return serializer.writeCode(buffer = CodeBlock.of(""), unpackedCode)
        }
        override fun unpackCodeJvm(packedCode: CodeBlock): CodeBlock {
            return serializer.readCode(packedCode)
//            return CodeBlock.of("%L.%M(%L)", initSerializer, TypeMatcher.Method.Unpack, packedCode)
        }

        override fun describe(): String {
            return kotlinType.toString()
        }
    }

    data class ByteBuffer(
        override val kotlinType: TypeName = TypeMatcher.KByteBuffer,
        override val jniType: JNIType = JNIType(TypeMatcher.KNioBuffer, TypeMatcher.JObject, "l"),
    ): TypeInfo() {
        override fun packCode(unpackedCode: CodeBlock): CodeBlock {
            // Create a jobject for common bytebuffer
            return CodeBlock.of("%L.%M(env)!!", unpackedCode, TypeMatcher.Method.ToJNioByteBuffer)
        }

        override fun unpackCode(packedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("%L.%M(env)!!", packedCode, TypeMatcher.Method.ToKDirectByteBuffer)
        }

        override fun packCodeJvm(unpackedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("%L.jvmBuffer", unpackedCode)
        }

        override fun unpackCodeJvm(packedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("%T(%L)", TypeMatcher.KByteBuffer, packedCode)
        }

        override fun describe(): String {
            return ""
        }
    }

    data class NativeInstance(
        override val kotlinType: TypeName
    ): TypeInfo() {
        override val jniType: JNIType = JNIType(LONG, TypeMatcher.JLong, "j")

        override fun packCode(unpackedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("%L.%M()", unpackedCode, TypeMatcher.Method.AsLongPointer)
        }

        override fun unpackCode(packedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("%L.%M<%T>()", packedCode, TypeMatcher.Method.FromLongPointer, kotlinType)
        }

        override fun packCodeJvm(unpackedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("%L", unpackedCode)
        }

        override fun unpackCodeJvm(packedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("%L", packedCode)
        }

        override fun describe(): String {
            return "jobject instance"
        }
    }

    data class Callback(
        override val kotlinType: TypeName,
    ): TypeInfo() {
        override val jniType: JNIType = JNIType(jvmType = kotlinType, nativeType = TypeMatcher.JObject, "l")

        override fun packCode(unpackedCode: CodeBlock): CodeBlock {
            return unpackedCode
        }

        override fun unpackCode(packedCode: CodeBlock): CodeBlock {
            return CodeBlock.of(
                "%T(env, %L)",
                kotlinType.getNativeImplClass(),
                packedCode
            )
        }

        override fun packCodeJvm(unpackedCode: CodeBlock): CodeBlock {
            return unpackedCode
        }

        override fun unpackCodeJvm(packedCode: CodeBlock): CodeBlock {
            return packedCode
        }

        override fun describe(): String {
            return "@JniCallback annotated jobject"
        }
    }
}