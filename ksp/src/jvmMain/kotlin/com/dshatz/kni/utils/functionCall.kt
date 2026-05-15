package com.dshatz.kni.utils

import com.dshatz.kni.callable.joinToString
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.withIndent

fun CodeBlock.Builder.callFunction(
    receiver: String,
    member: MemberName,
    parameters: FunCallParamScope.() -> Unit
): CodeBlock.Builder {
    val scope = FunCallParamScope()
    scope.parameters()
    val params = scope.params
    val joined = params.joinToString(", \n")
    val receiver = if (receiver.isEmpty()) CodeBlock.of("") else CodeBlock.of("%L.", receiver)
    return add("%L%M(\n", receiver, member).withIndent {
        add(joined)
    }.add(")")
}

class FunCallParamScope {
    val params = mutableListOf<CodeBlock>()
    fun lambdaParam(name: String, create: LambdaParamScope.() -> CodeBlock) {
        val scope = LambdaParamScope()
        val code = scope.create()
        val lambda = CodeBlock.builder().beginControlFlow("")
            .add("%L\n", code)
            .endControlFlow().build()
        named(name, lambda)
    }

    fun named(name: String, value: CodeBlock) {
        params.add(CodeBlock.of("%N = %L", name, value))
    }

    class LambdaParamScope {
        val it = CodeBlock.of("it")
    }
}

fun CodeBlock.asReceiver(): CodeBlock {
    if (isEmpty()) return this
    else return CodeBlock.of("%L.", this)
}