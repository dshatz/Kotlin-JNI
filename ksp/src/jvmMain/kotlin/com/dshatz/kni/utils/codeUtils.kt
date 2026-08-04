package com.dshatz.kni.utils

import com.dshatz.kni.TypeInfo
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile

fun CodeBlock.Builder.define(name: String, type: TypeInfo, template: String, vararg args: Any?): ValDefinition {
    val definition = addStatement("val %N = %L", name, CodeBlock.of(template, args = args)).build()
        .returnType(type)
    val ref = CodeBlock.of("%N", name).returnType(type)
    return ValDefinition(definition, ref)
}

fun TypeSpec.Builder.originatesFrom(declaration: KSDeclaration): TypeSpec.Builder {
    return declaration.containingFile?.let(::addOriginatingKSFile) ?: this
}

fun FunSpec.Builder.originatesFrom(declaration: KSFunctionDeclaration): FunSpec.Builder {
    return declaration.containingFile?.let(::addOriginatingKSFile) ?: this
}

data class ValDefinition(
    val definition: TypedCodeMP,
    val reference: TypedCodeMP
)