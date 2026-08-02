package com.dshatz.kni.utils

import com.squareup.kotlinpoet.CodeBlock
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