package com.dshatz.kni.kspfix

import com.dshatz.kni.model.KSConstructor
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.model.PropInfo
import com.dshatz.kni.model.WithParent
import com.dshatz.kni.model.flow.KSFlowProp
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import jdk.javadoc.internal.doclets.toolkit.util.DocPath.parent

sealed class FunctionParent {
    abstract val classNameKt: ClassName
    abstract val className: ClassName

    interface WithVisibility {
        val modifier: KModifier?
    }

    abstract fun member(simpleName: String): MemberName
    data class Class(
        override val className: ClassName,
        val superTypes: List<TypeName>,
        override val modifier: KModifier? = null
    ): FunctionParent(), WithVisibility {
        override val classNameKt: ClassName = className
        override fun member(simpleName: String): MemberName {
            return MemberName(className, simpleName)
        }
    }

    data class Object(
        override val className: ClassName,
        override val modifier: KModifier? = null
    ): FunctionParent(), WithVisibility {
        override val classNameKt: ClassName = className
        override fun member(simpleName: String): MemberName {
            return MemberName(className, simpleName)
        }
    }

    data class TopLevel(
        override val className: ClassName,
        override val classNameKt: ClassName
    ): FunctionParent() {
        override fun member(simpleName: String): MemberName {
            return MemberName(className.packageName, simpleName)
        }

    }
}