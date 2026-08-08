package com.dshatz.kni.utils

import com.dshatz.kni.TypeInfo
import com.dshatz.kni.Types
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.sun.tools.javac.tree.TreeInfo.args

private fun <T: TypedCodeMP> CodeBlock.Builder.define(name: String, template: String, vararg args: Any?, toTyped: CodeBlock.() -> T): ValDefinition<T> {
    val definition = addStatement("val %N = %L", name, CodeBlock.of(template, args = args)).build().toTyped()
    val ref = CodeBlock.of("%N", name).toTyped()
    return ValDefinition(definition, ref)
}

fun CodeBlock.Builder.defineJvm(name: String, type: TypeInfo, template: String, vararg args: Any?): ValDefinition<TypedCodeMP.JVM> {
    return define(name, template, args = args) {
        jvmCode(type)
    }
}

fun CodeBlock.Builder.defineCommon(name: String, type: TypeInfo, template: String, vararg args: Any?): ValDefinition<TypedCodeMP.Common> {
    return define(name, template, args = args) {
        commonCode(type)
    }
}

fun CodeBlock.Builder.defineNative(name: String, type: TypeInfo, template: String, vararg args: Any?): ValDefinition<TypedCodeMP.Native> {
    return define(name, template, args = args) {
        nativeCode(type)
    }
}


fun TypeSpec.Builder.originatesFrom(declaration: KSDeclaration): TypeSpec.Builder {
    return declaration.containingFile?.let(::addOriginatingKSFile) ?: this
}

fun FunSpec.Builder.originatesFrom(declaration: KSFunctionDeclaration): FunSpec.Builder {
    return declaration.containingFile?.let(::addOriginatingKSFile) ?: this
}

data class ValDefinition<T: TypedCodeMP>(
    val definition: TypedCodeMP,
    val reference: T
)

fun cnameFunBuilder(
    funName: String,
    cname: String
): FunSpec.Builder {
    return FunSpec.builder(funName)
        .addAnnotation(AnnotationSpec.builder(Types.Annotations.CName).addMember(CodeBlock.of("%S", cname)).build())
        .addAnnotation(Types.Annotations.Optin.NativeOptIn)
        .addParameter("env", Types.Environment)
        .addParameter("clazz", Types.JObject)
}

fun cnameFunBuilder(
    callFun: MemberName,
    jniCname: String,
): FunSpec.Builder {
    return cnameFunBuilder(
        funName = "_${callFun.enclosingClassName?.simpleName.orEmpty()}_${callFun.simpleName}JNI",
        cname = jniCname
    ).addKdoc("Calling user function [$callFun]")
}

fun List<CodeBlock>.joinToString(separator: String = ", "): CodeBlock {
    return CodeBlock.of(
        generateSequence { "%L" }.take(size).joinToString(separator),
        args = this.toTypedArray()
    )
}