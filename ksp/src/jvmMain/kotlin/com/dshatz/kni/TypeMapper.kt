package com.dshatz.kni

import com.dshatz.kni.callable.getNativeImplClass
import com.dshatz.kni.utils.dereferenceTypeAlias
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName

internal class TypeMapper(
    private val registry: Registry
) {
    fun mapType(
        type: KSTypeReference,
    ): TypeInfo {
        val kotlinType = type.dereferenceTypeAlias().toTypeName()
        return if (kotlinType in TypeMatcher.jTypes) {
            // has an existing j type
            val jType = TypeMatcher.jTypes[kotlinType]!!
            if (kotlinType in TypeMatcher.toJTypes && jType in TypeMatcher.toKTypes) {
                // We have converters to and from
                TypeInfo.Convertable(
                    kotlinType = kotlinType,
                    jniType = JNIType(kotlinType, jType),
                    toJni = TypeMatcher.toJTypes[kotlinType]!!,
                    fromJni = TypeMatcher.toKTypes[jType]!!
                )
            } else {
                // No converter, so it is a primitive - e.g. jfloat
                TypeInfo.Simple(
                    kotlinType = kotlinType,
                    jniType = JNIType(jvmType = kotlinType, jType)
                )
            }
        } else if (kotlinType in registry.serializers) {
            // custom serializer defined
            val serializer = registry.serializers[kotlinType]!!
            TypeInfo.Serializable(
                kotlinType = kotlinType,
                jniType = JNIType(TypeMatcher.KByteArray, TypeMatcher.JByteArray),
                serializer = serializer
            )
        } else if (kotlinType in registry.callables) {
            TypeInfo.Callback(kotlinType)
        } else if (kotlinType == TypeMatcher.KByteBuffer) {
            TypeInfo.ByteBuffer()
        } else {
            TypeInfo.Simple(
                kotlinType = kotlinType,
                jniType = JNIType(kotlinType, kotlinType)
            )
        }
    }
}


data class JNIType(val jvmType: TypeName, val nativeType: TypeName)

sealed class TypeInfo {
    abstract val kotlinType: TypeName
    abstract val jniType: JNIType

    abstract fun packCode(unpackedCode: CodeBlock): CodeBlock
    abstract fun unpackCode(packedCode: CodeBlock): CodeBlock
    abstract fun packCodeJvm(unpackedCode: CodeBlock): CodeBlock
    abstract fun unpackCodeJvm(packedCode: CodeBlock): CodeBlock

    data class Simple(
        override val kotlinType: TypeName,
        override val jniType: JNIType = JNIType(kotlinType, kotlinType)
    ): TypeInfo() {
        override fun packCode(unpackedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("%L", unpackedCode)
        }
        override fun unpackCode(packedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("%L", packedCode)
        }
        override fun packCodeJvm(unpackedCode: CodeBlock): CodeBlock = unpackedCode
        override fun unpackCodeJvm(packedCode: CodeBlock): CodeBlock = packedCode
    }

    data class Convertable(
        override val kotlinType: TypeName,
        override val jniType: JNIType,
        val toJni: MemberName,
        val fromJni: MemberName
    ): TypeInfo() {
        override fun packCode(unpackedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("(%L).%M(env)!!", unpackedCode, toJni)
        }
        override fun unpackCode(packedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("(%L).%M(env)!!", packedCode, fromJni)
        }
        override fun packCodeJvm(unpackedCode: CodeBlock): CodeBlock = unpackedCode
        override fun unpackCodeJvm(packedCode: CodeBlock): CodeBlock = packedCode
    }

    data class Serializable(
        override val kotlinType: TypeName,
        override val jniType: JNIType = JNIType(TypeMatcher.KByteArray, TypeMatcher.JByteArray),
        val serializer: ClassName
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
            return CodeBlock.of("%T.%M(%L)", serializer, TypeMatcher.Method.Pack, unpackedCode)
        }
        override fun unpackCodeJvm(packedCode: CodeBlock): CodeBlock {
            return CodeBlock.of("%T.%M(%L)", serializer, TypeMatcher.Method.Unpack, packedCode)
        }
    }

    data class ByteBuffer(
        override val kotlinType: TypeName = TypeMatcher.KByteBuffer,
        override val jniType: JNIType = JNIType(TypeMatcher.KNioBuffer, TypeMatcher.JObject),
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
    }

    data class Callback(
        override val kotlinType: TypeName,
    ): TypeInfo() {
        override val jniType: JNIType = JNIType(jvmType = kotlinType, nativeType = TypeMatcher.JObject)

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
    }
}