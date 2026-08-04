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

fun TypedCode.add(typedCode: TypedCode): TypedCode {
    return CodeBlock.of("%L%L", code, typedCode.code).returnType(typedCode.type)
}

data class TypedCodeMP(
    val code: CodeBlock,
    val type: TypeInfo
) {
    fun packCode() = type.packCode(code.returnType(type.kotlinType))
    fun unpackCode() = type.unpackCode(code.returnType(type.jniType.nativeType))
    fun packCodeJvm() = type.packCodeJvm(code.returnType(type.kotlinType))
    fun unpackCodeJvm() = type.unpackCodeJvm(code.returnType(type.jniType.jvmType))
}

fun CodeBlock.returnType(type: TypeInfo) = TypedCodeMP(this, type)
fun CodeBlock.Builder.add(code: TypedCodeMP) = add(code.code)
/*fun CodeBlock.Builder.add(codes: Collection<TypedCode>): CodeBlock.Builder = apply {
    codes.forEach { add(it.code) }
}*/
fun CodeBlock.Builder.addStatement(code: TypedCodeMP) = addStatement("%L", code.code)

fun FunSpec.Builder.addCode(code: TypedCodeMP) = addCode(code.code)
fun FunSpec.Builder.addReturn(code: TypedCode) = addStatement("return %L", code.code).returns(code.type)