package com.dshatz.kni.kspfix

import com.dshatz.kni.model.KSConstructor
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.model.PropInfo
import com.dshatz.kni.model.WithParent
import com.dshatz.kni.model.flow.KSFlowProp
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import jdk.javadoc.internal.doclets.toolkit.util.DocPath.parent

sealed class FunctionParent {
    abstract val declaration: KSNode
    abstract val classNameKt: ClassName
    abstract val className: ClassName
    data class Class(
        override val declaration: KSClassDeclaration,
        override val className: ClassName,
        val constructors: List<KSConstructor>,
        val superTypes: List<TypeName>,
        val props: List<PropInfo>
    ): FunctionParent() {
        override val classNameKt: ClassName = className
    }

    data class Object(
        override val declaration: KSDeclaration,
        override val className: ClassName
    ): FunctionParent() {
        override val classNameKt: ClassName = className
    }

    data class TopLevel(
        override val declaration: KSFile,
        override val className: ClassName,
        override val classNameKt: ClassName
    ): FunctionParent()

    fun toMemberName(funName: String): MemberName {
        return if (this is TopLevel) MemberName(className.packageName, funName)
        else MemberName(className, funName)
    }
}