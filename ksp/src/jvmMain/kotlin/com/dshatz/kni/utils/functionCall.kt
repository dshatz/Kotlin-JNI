package com.dshatz.kni.utils

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.withIndent

fun TypedCode.callFunction(
    member: MemberName,
    returnType: TypeName,
    parameters: FunCallParamScope.() -> Unit
): TypedCode {
    val scope = FunCallParamScope()
    scope.parameters()
    val params = scope.params
    val joined = params.joinToString(", \n")
    val callOnReceiver = CodeBlock.builder()
        .add("%M(\n", member)
        .withIndent {
            add(joined)
        }
        .add(")")
        .build()
        .returnType(returnType.copy(nullable = this.type.isNullable))
    return nullSafeCall(callOnReceiver)
}

class FunCallParamScope {
    val params = mutableListOf<CodeBlock>()
    fun lambdaParam(
        name: String,
        receiverType: TypeName,
        create: LambdaParamScope.() -> CodeBlock
    ) {
        val scope = LambdaParamScope(receiverType)
        val code = scope.create()
        makeLambdaParam(name, code)
    }

    fun lambdaParam(
        name: String,
        argumentType: TypeName,
        receiverType: TypeName,
        create: LambdaParamWithArgument.() -> CodeBlock
    ) {
        val scope = LambdaParamWithArgument(receiverType, argumentType)
        val code = scope.create()
        makeLambdaParam(name, code)
    }

    private fun makeLambdaParam(paramName: String, code: CodeBlock) {
        val lambda = CodeBlock.builder().beginControlFlow("")
            .add("%L\n", code)
            .endControlFlow().build()
        named(paramName, lambda)
    }

    fun named(name: String, value: CodeBlock) {
        params.add(CodeBlock.of("%N = %L", name, value))
    }


    open class LambdaParamScope(receiverType: TypeName) {
        val `this` = CodeBlock.of("this").returnType(receiverType)
    }

    class LambdaParamWithArgument(
        receiverType: TypeName,
        argumentType: TypeName,
    ): LambdaParamScope(argumentType) {
        val it = CodeBlock.of("it").returnType(argumentType)
    }

}

fun CodeBlock.asReceiver(): CodeBlock {
    if (isEmpty()) return this
    else return CodeBlock.of("%L.", this)
}