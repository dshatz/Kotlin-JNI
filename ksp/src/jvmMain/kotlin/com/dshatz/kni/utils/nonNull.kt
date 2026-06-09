package com.dshatz.kni.utils

import com.dshatz.kni.Types
import com.dshatz.kni.needsIsNullParam
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.joinToCode

fun TypeName.notNullable() = copy(nullable = false)

fun TypedCode.nonNullOrPlaceholder(): TypedCode {
    return if (type.needsIsNullParam()) {
        CodeBlock.of("%L ?: %L", this.code, Types.boxedWhenNullable[type.notNullable()])
            .returnType(type.notNullable())
    } else this
}

fun TypedCode.checkNotNull(useNonNullValue: (CodeBlock) -> TypedCode): TypedCode {
    val resultCode = useNonNullValue(code)
    return if (type.isNullable) {
        CodeBlock.of("if (%L == null) null else %L", code, resultCode.code).returnType(resultCode.type)
    } else useNonNullValue(code)
}

fun TypedCode.nullSafeCall(callOnNonNull: TypedCode): TypedCode {
    return CodeBlock.of("%L%L%L", code, nullCheck, callOnNonNull.code).returnType(callOnNonNull.type.copy(nullable = type.isNullable))
}

fun Iterable<TypedCode>.joinToCode(separator: String = ", "): CodeBlock {
    return map { it.code }.joinToCode(separator)
}