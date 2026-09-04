package com.dshatz.kni.utils

import com.dshatz.kni.Types
import com.dshatz.kni.needsIsNullParam
import com.dshatz.kni.processors.TypedMember
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.joinToCode

fun TypeName.notNullable() = copy(nullable = false)
fun ClassName.notNullable(): ClassName = this.copy(nullable = false) as ClassName

fun TypedCode.nonNullOrPlaceholder(): TypedCode {
    return if (type.needsIsNullParam()) {
        CodeBlock.of("(%L ?: %L)", this.code, Types.boxedWhenNullable[type.notNullable()])
            .returnType(type.notNullable())
    } else this
}

fun TypedCode.nullSafeCall(callOnNonNull: TypedCode): TypedCode {
    return CodeBlock.of("%L%L%L", code, nullCheck, callOnNonNull.code).returnType(callOnNonNull.type.copy(nullable = type.isNullable))
}

fun TypedCode.nullSafeCall(member: TypedMember): TypedCode {
    return CodeBlock.of("%L%L%M(%L)", code, nullCheck, member.memberName, member.params).returnType(member.type)
}

fun Iterable<TypedCode>.joinToCode(separator: String = ", "): CodeBlock {
    return map { it.code }.joinToCode(separator)
}