package com.dshatz.kni.serialization

import com.dshatz.kni.Registry
import com.dshatz.kni.Types
import com.dshatz.kni.Types.typeOf
import com.dshatz.kni.serialization.IncludedSerializers.Serializer.Extension.Companion.kniExtension
import com.dshatz.kni.utils.TypedCode
import com.dshatz.kni.utils.asReceiver
import com.dshatz.kni.utils.callFunction
import com.dshatz.kni.utils.capitalized
import com.dshatz.kni.utils.nullSafeCall
import com.dshatz.kni.utils.returnType
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BOOLEAN_ARRAY
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.DOUBLE_ARRAY
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FLOAT_ARRAY
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LONG_ARRAY
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.SHORT_ARRAY
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.U_BYTE
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import com.squareup.kotlinpoet.U_INT
import com.squareup.kotlinpoet.U_INT_ARRAY
import com.squareup.kotlinpoet.U_LONG
import com.squareup.kotlinpoet.U_LONG_ARRAY
import com.squareup.kotlinpoet.U_SHORT
import com.squareup.kotlinpoet.U_SHORT_ARRAY

class IncludedSerializers(
    val registry: Registry,
    val logger: KSPLogger
) {
    @Throws(NoSerializerException::class)
    context(decl: KSNode)
    fun serializer(type: TypeName, overrideSerializer: ClassName? = null): Serializer {
        if (overrideSerializer != null) {
            return Serializer.StaticObject(type, overrideSerializer)
        }
        return when (type) {
            is ParameterizedTypeName -> {
                val rawType = type.rawType
                if (collections.keys.any { rawType typeOf it }) {
                    // collection
                    val itemType = type.typeArguments.first()
                    Serializer.Collection(type, serializer(itemType), collections[rawType]!!, itemType)
                } else if (rawType typeOf MAP) {
                    Serializer.Map(
                        keySerializer = serializer(type.typeArguments[0]),
                        valueSerializer = serializer(type.typeArguments[1]),
                        keyType = type.typeArguments[0],
                        valueType = type.typeArguments[1],
                        type = type
                    )
                } else {
                    val rawSerializer = serializer(rawType)
                    val paramSerializers = type.typeArguments.map { serializer(it) }
                    Serializer.Generic(
                        type,
                        rawSerializer,
                        paramSerializers
                    )
                }
            }
            in setOf(BYTE, SHORT, INT, LONG) -> {
                Serializer.KioBufferMethod(type, (type as ClassName).simpleName)
            }
            in kioSupported -> {
                Serializer.kioExtension(type)
            }
            in defined -> {
                defined[type]!!
            }
            in arrays -> {
                Serializer.Array(type, serializer(arrays[type]!!), type)
            }
            in registry.serializers -> {
                Serializer.StaticObject(type as ClassName, registry.serializers[type]!!.serializer)
            }
            else -> throw NoSerializerException(type)
        }
    }

    sealed class Serializer {
        abstract val type: TypeName
        companion object {
            fun kioExtension(type: TypeName): Extension {
                val typeName = (type as ClassName).simpleName
                return Extension(
                    type,
                    MemberName("kotlinx.io", "read${typeName.capitalized()}"),
                    MemberName("kotlinx.io", "write${typeName.capitalized()}"),
                )
            }
        }

        protected abstract fun readCodeInternal(buffer: TypedCode): CodeBlock
        protected abstract fun writeCodeInternal(buffer: CodeBlock, value: TypedCode): CodeBlock

        fun readCode(buffer: TypedCode): TypedCode = readCodeInternal(buffer).returnType(type.copy(nullable = buffer.type.isNullable))
        fun writeCode(buffer: CodeBlock, value: TypedCode): TypedCode = writeCodeInternal(buffer, value).returnType(Types.KByteArray.copy(nullable = value.type.isNullable))


        data class StaticObject(
            override val type: TypeName,
            val serializer: ClassName = type.serializerClass()
        ): Serializer() {

            override fun readCodeInternal(buffer: TypedCode): CodeBlock {
                return buffer.nullSafeCall(
                    CodeBlock.of("%M(%T)", Types.Method.Deserialize, serializer)
                    .returnType(type)
                ).code
            }

            override fun writeCodeInternal(buffer: CodeBlock, value: TypedCode): CodeBlock {
                return if (buffer.isEmpty()) {
                    value.nullSafeCall(
                        CodeBlock.of("%M(%T)", Types.Method.Serialize, serializer).returnType(Types.KByteArray)
                    ).code
                } else {
                    CodeBlock.of("%T.packTo(%L, %L)", serializer, value.code, buffer)
                }
            }

        }

        data class KioBufferMethod(
            override val type: TypeName,
            val typeName: String,
        ): Serializer() {
            override fun readCodeInternal(buffer: TypedCode): CodeBlock {
                return buffer.nullSafeCall(
                    CodeBlock.of("read${typeName.capitalized()}()").returnType(type)
                ).code
            }
            override fun writeCodeInternal(buffer: CodeBlock, value: TypedCode): CodeBlock {
                return CodeBlock.of("%L%L(%L)", buffer.asReceiver(), "write${typeName.capitalized()}", value.code)
            }
        }
        data class Extension(
            override val type: TypeName,
            val read: MemberName,
            val write: MemberName
        ) : Serializer() {
            override fun readCodeInternal(buffer: TypedCode): CodeBlock {
                return buffer.nullSafeCall(
                    CodeBlock.of("%M()", read).returnType(type)
                ).code
            }

            override fun writeCodeInternal(buffer: CodeBlock, value: TypedCode): CodeBlock {
                return CodeBlock.of("%L%M(%L)", buffer.asReceiver(), write, value.code)
            }

            companion object {
                fun kniExtension(type: TypeName, typeName: String): Extension {
                    return Extension(
                        read = MemberName("com.dshatz.kni.serialization", "read${typeName.capitalized()}"),
                        write = MemberName("com.dshatz.kni.serialization", "write${typeName.capitalized()}"),
                        type = type
                    )
                }
            }
        }

        data class Array(override val type: TypeName, val inner: Serializer, val targetType: TypeName): Serializer() {
            override fun readCodeInternal(buffer: TypedCode): CodeBlock {
                return buffer.callFunction(MemberName("kotlin", "run"), returnType = targetType) {
                    lambdaParam("block", receiverType = buffer.type) {
                        CodeBlock.builder()
                            .addStatement("val arr = %T(readInt())", targetType)
                            .beginControlFlow("for (i in arr.indices)")
                            .addStatement("arr[i] = %L", inner.readCodeInternal(CodeBlock.of("this").returnType(Types.IoBuffer)))
                            .endControlFlow()
                            .addStatement("arr")
                            .build()
                    }
                }.code
            }

            override fun writeCodeInternal(
                buffer: CodeBlock,
                value: TypedCode
            ): CodeBlock {
                return CodeBlock.builder().beginControlFlow("%L.run", buffer)
                    .addStatement("writeInt(%L.size)", value.code)
                    .beginControlFlow("for (v in %L)", value.code)
                    // targetType is not correct - actually array item type.
                    .addStatement("%L", inner.writeCodeInternal(buffer, CodeBlock.of("v").returnType(targetType)))
                    .endControlFlow()
                    .endControlFlow()
                    .build()
            }

        }


        data class Collection(override val type: TypeName, val inner: Serializer, val toTarget: MemberName, val itemType: TypeName): Serializer() {
            companion object {
                private val read = MemberName("com.dshatz.kni.serialization", "readList")
                private val write = MemberName("com.dshatz.kni.serialization", "writeList")
            }
            override fun readCodeInternal(buffer: TypedCode): CodeBlock {
                return buffer.callFunction(read, type) {
                    lambdaParam("readItem", receiverType = Types.IoBuffer) {
                        inner.readCodeInternal(`this`)
                    }
                }.nullSafeCall(CodeBlock.of("%M()", toTarget).returnType(type)).code
            }

            override fun writeCodeInternal(buffer: CodeBlock, value: TypedCode): CodeBlock {
                return value.callFunction(write, Types.KByteArray) {
                    named("buffer", buffer)
                    lambdaParam("writeItem", receiverType = Types.IoBuffer, argumentType = itemType) {
                        inner.writeCodeInternal(`this`.code, it)
                    }
                }.code
            }

        }

        data class Map(
            override val type: TypeName,
            val keySerializer: Serializer,
            val valueSerializer: Serializer,
            val keyType: TypeName,
            val valueType: TypeName
        ): Serializer() {
            companion object {
                private val read = MemberName("com.dshatz.kni.serialization", "readMap")
                private val write = MemberName("com.dshatz.kni.serialization", "writeMap")
            }
            override fun readCodeInternal(buffer: TypedCode): CodeBlock {
                return buffer.callFunction(read, type) {
                    lambdaParam("readKey", Types.IoBuffer) {
                        keySerializer.readCodeInternal(`this`)
                    }

                    lambdaParam("readValue", Types.IoBuffer) {
                        valueSerializer.readCodeInternal(`this`)
                    }
                }.code
            }

            override fun writeCodeInternal(buffer: CodeBlock, value: TypedCode): CodeBlock {
                return value.callFunction(write, type) {
                    named("buffer", buffer)
                    lambdaParam("writeKey", argumentType = keyType, receiverType = Types.IoBuffer) {
                        keySerializer.writeCodeInternal(`this`.code, it)
                    }
                    lambdaParam("writeValue", argumentType = valueType, receiverType = Types.IoBuffer) {
                        valueSerializer.writeCodeInternal(`this`.code, it)
                    }
                }.code
            }

        }


        data class Generic(
            override val type: ParameterizedTypeName,
            val rawSerializer: Serializer,
            val argumentSerializer: List<Serializer>
        ): Serializer() {
            val serializerClass = type.genericSerializerName()
            private val serializer = StaticObject(type, serializerClass)
            override fun readCodeInternal(buffer: TypedCode): CodeBlock {
                return serializer.readCode(buffer).code
            }

            override fun writeCodeInternal(buffer: CodeBlock, value: TypedCode): CodeBlock {
                return serializer.writeCode(buffer, value).code
            }
        }
    }

    companion object {
        val kioSupported = listOf(
            BYTE,
            SHORT,
            INT,
            LONG,
            FLOAT,
            DOUBLE
        )

        val collections = mapOf(
            LIST to MemberName("kotlin.collections", "toList"),
            SET to MemberName("kotlin.collections", "toSet")
        )

        val defined = mapOf(
            STRING to kniExtension(STRING, "lenString"),
            BOOLEAN to kniExtension(BOOLEAN, "bool"),
            BYTE_ARRAY to kniExtension(BYTE_ARRAY, "lenBytes"),
            CHAR to kniExtension(CHAR, "char"),
            UNIT to kniExtension(UNIT, "unit")
        )

        // Do not include ByteArray here, it is written directly.
        val arrays = mapOf(
            BOOLEAN_ARRAY to BOOLEAN,
            CHAR_ARRAY to CHAR,
            SHORT_ARRAY to SHORT,
            INT_ARRAY to INT,
            LONG_ARRAY to LONG,
            FLOAT_ARRAY to FLOAT,
            DOUBLE_ARRAY to DOUBLE,
            U_BYTE_ARRAY to U_BYTE,
            U_SHORT_ARRAY to U_SHORT,
            U_INT_ARRAY to U_INT,
            U_LONG_ARRAY to U_LONG
        )
    }
}

class NoSerializerException(type: TypeName): Exception("No serializer defined for $type")