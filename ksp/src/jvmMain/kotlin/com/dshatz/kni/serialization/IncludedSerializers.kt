package com.dshatz.kni.serialization

import com.dshatz.kni.Registry
import com.dshatz.kni.Types.typeOf
import com.dshatz.kni.serialization.IncludedSerializers.Serializer.Extension.Companion.kniExtension
import com.dshatz.kni.utils.asReceiver
import com.dshatz.kni.utils.callFunction
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName

class IncludedSerializers(
    val registry: Registry,
    val logger: KSPLogger
) {
    fun serializer(type: TypeName): Serializer {
        return when (type) {
            is ParameterizedTypeName -> {
                val rawType = type.rawType
                if (collections.keys.any { rawType typeOf it }) {
                    // collection
                    Serializer.Collection(serializer(type.typeArguments.first()), collections[rawType]!!)
                } else if (rawType typeOf MAP) {
                    Serializer.Map(
                        keySerializer = serializer(type.typeArguments[0]),
                        valueSerializer = serializer(type.typeArguments[1])
                    )
                } else {
                    val rawSerializer = serializer(rawType)
                    val paramSerializers = type.typeArguments.map { serializer(it) }
//                    error("Don't know how to serialize parameterized $type $rawSerializer <$paramSerializers>")
//                    registry.genericSerializers[type] = type.genericSerializerName()
                    Serializer.Generic(
                        rawSerializer,
                        paramSerializers,
                        type
                    ).also { registry.genericSerializers += it }
                }
            }
            in setOf(BYTE, SHORT, INT, LONG) -> {
                Serializer.KioBufferMethod((type as ClassName).simpleName)
            }
            in kioSupported -> {
                Serializer.kioExtension((type as ClassName).simpleName)
            }
            in defined -> {
                defined[type]!!
            }
            in registry.serializers -> {
                Serializer.StaticObject(type as ClassName, registry.serializers[type]!!)
            }
            else -> throw NoSerializerException(type)
        }
    }

    sealed class Serializer {
        companion object {
            fun kioExtension(typeName: String): Extension {
                return Extension(
                    MemberName("kotlinx.io", "read${typeName.capitalize()}"),
                    MemberName("kotlinx.io", "write${typeName.capitalize()}")
                )
            }
        }

        abstract fun readCode(buffer: CodeBlock): CodeBlock
        abstract fun writeCode(buffer: CodeBlock, value: CodeBlock): CodeBlock



        data class StaticObject(
            val type: TypeName,
            val serializer: ClassName = type.serializerClass()
        ): Serializer() {

            override fun readCode(buffer: CodeBlock): CodeBlock {
                return CodeBlock.of("%T.unpackFrom(%L)", serializer, buffer)
            }

            override fun writeCode(buffer: CodeBlock, value: CodeBlock): CodeBlock {
                return if (buffer.isEmpty()) {
                    CodeBlock.of("%T.pack(%L)", serializer, value)
                } else {
                    CodeBlock.of("%T.packTo(%L, %L)",serializer, value, buffer)
                }
            }

        }

        data class KioBufferMethod(
            val typeName: String,
        ): Serializer() {
            override fun readCode(buffer: CodeBlock): CodeBlock {
                return CodeBlock.of("%L%L()", buffer.asReceiver(), "read${typeName.capitalize()}")
            }
            override fun writeCode(buffer: CodeBlock, value: CodeBlock): CodeBlock {
                return CodeBlock.of("%L%L(%L)", buffer.asReceiver(), "write${typeName.capitalize()}", value)
            }
        }
        data class Extension(
            val read: MemberName,
            val write: MemberName
        ) : Serializer() {
            override fun readCode(buffer: CodeBlock): CodeBlock {
                return CodeBlock.of("%L%M()", buffer.asReceiver(), read)
            }

            override fun writeCode(buffer: CodeBlock, value: CodeBlock): CodeBlock {
                return CodeBlock.of("%L%M(%L)", buffer.asReceiver(), write, value)
            }

            companion object {
                fun kniExtension(typeName: String): Extension {
                    return Extension(
                        read = MemberName("com.dshatz.kni.serialization", "read${typeName.capitalize()}"),
                        write = MemberName("com.dshatz.kni.serialization", "write${typeName.capitalize()}"),
                    )
                }
            }
        }

        data class Collection(val inner: Serializer, val toTarget: MemberName): Serializer() {
            companion object {
                private val read = MemberName("com.dshatz.kni.serialization", "readList")
                private val write = MemberName("com.dshatz.kni.serialization", "writeList")
            }
            override fun readCode(buffer: CodeBlock): CodeBlock {
                return CodeBlock.builder().callFunction(buffer.toString(), read) {
                    lambdaParam("readItem") {
                        inner.readCode(CodeBlock.of(""))
                    }
                }.add(".%M()", toTarget).build()
            }

            override fun writeCode(buffer: CodeBlock, value: CodeBlock): CodeBlock {
                return CodeBlock.builder().callFunction("buffer", write) {
                    named("col", value)
                    lambdaParam("writeItem") {
                        inner.writeCode(CodeBlock.of(""), it)
                    }
                }.build()
            }

        }

        data class Map(val keySerializer: Serializer, val valueSerializer: Serializer): Serializer() {
            companion object {
                private val read = MemberName("com.dshatz.kni.serialization", "readMap")
                private val write = MemberName("com.dshatz.kni.serialization", "writeMap")
            }
            override fun readCode(buffer: CodeBlock): CodeBlock {
                return CodeBlock.builder().callFunction(buffer.toString(), read) {
                    lambdaParam("readKey") {
                        keySerializer.readCode(CodeBlock.of(""))
                    }

                    lambdaParam("readValue") {
                        valueSerializer.readCode(CodeBlock.of(""))
                    }
                }.build()
            }

            override fun writeCode(buffer: CodeBlock, value: CodeBlock): CodeBlock {
                return CodeBlock.builder().callFunction("buffer", write) {
                    named("map", value)
                    lambdaParam("writeKey") {
                        keySerializer.writeCode(CodeBlock.of(""), it)
                    }
                    lambdaParam("writeValue") {
                        valueSerializer.writeCode(CodeBlock.of(""), it)
                    }
                }.build()
            }

        }


        data class Generic(
            val rawSerializer: Serializer,
            val argumentSerializer: List<Serializer>,
            val kotlinType: ParameterizedTypeName
        ): Serializer() {
            private val serializer = Serializer.StaticObject(kotlinType, kotlinType.genericSerializerName())
            override fun readCode(buffer: CodeBlock): CodeBlock {
                return serializer.readCode(buffer)
            }

            override fun writeCode(buffer: CodeBlock, value: CodeBlock): CodeBlock {
                return serializer.writeCode(buffer, value)
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
            STRING to kniExtension("lenString")
        )
    }
}

class NoSerializerException(type: TypeName): Exception("No serializer defined for $type")