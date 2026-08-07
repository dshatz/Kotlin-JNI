package com.dshatz.kni.utils

import com.dshatz.kni.TypeInfo
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName

data class TypedCode(
    val code: CodeBlock,
    val type: TypeName,
) {
    val nullCheck: CodeBlock get() = if (type.isNullable) CodeBlock.of("?.") else CodeBlock.of(".")
}

fun CodeBlock.returnType(type: TypeName) = TypedCode(this, type)

sealed class TypedCodeMP {
    abstract val code: CodeBlock
    abstract val type: TypeInfo

    abstract val baseTypeName: TypeName
    val typeName: TypeName by lazy { baseTypeName.copy(nullable = nullable) }
    abstract val nullable: Boolean

    val typed: TypedCode by lazy { code.returnType(baseTypeName.copy(nullable = nullable)) }

    data class Common(
        override val code: CodeBlock,
        override val type: TypeInfo,
        override val nullable: Boolean
    ): TypedCodeMP() {
        override val baseTypeName: TypeName = type.kotlinType
        fun packToNative(): Native = Native(type.packCode(typed).code, type, nullable)
        fun packToJvm(): JVM = JVM(type.packCodeJvm(typed).code, type, nullable)
    }

    data class JVM(
        override val code: CodeBlock,
        override val type: TypeInfo,
        override val nullable: Boolean
    ): TypedCodeMP() {
        override val baseTypeName: TypeName = type.jniType.jvmType
        fun unpackCode(): Common = Common(type.unpackCodeJvm(typed).code, type, nullable)
    }

    data class Native(
        override val code: CodeBlock,
        override val type: TypeInfo,
        override val nullable: Boolean
    ): TypedCodeMP() {
        override val baseTypeName: TypeName = type.jniType.nativeType

        fun unpackCode(): Common = Common(type.unpackCode(typed).code, type, nullable)
    }
}

fun CodeBlock.Builder.add(code: TypedCodeMP) = add(code.code)
fun CodeBlock.Builder.addStatement(code: TypedCodeMP) = addStatement("%L", code.code)

fun FunSpec.Builder.addCode(code: TypedCodeMP) = addCode(code.code)
fun FunSpec.Builder.addReturn(code: TypedCode) = addStatement("return %L", code.code).returns(code.type)
fun FunSpec.Builder.addReturn(code: TypedCodeMP) = addStatement("return %L", code.code).returns(code.baseTypeName)

fun CodeBlock.jvmCode(type: TypeInfo, nullable: Boolean = type.jniType.jvmType.isNullable): TypedCodeMP.JVM {
    return TypedCodeMP.JVM(this, type, nullable)
}
fun CodeBlock.nativeCode(type: TypeInfo, nullable: Boolean = type.jniType.nativeType.isNullable): TypedCodeMP.Native {
    return TypedCodeMP.Native(this, type, nullable)
}
fun CodeBlock.commonCode(type: TypeInfo, nullable: Boolean = type.kotlinType.isNullable): TypedCodeMP.Common {
    return TypedCodeMP.Common(this, type, nullable)
}