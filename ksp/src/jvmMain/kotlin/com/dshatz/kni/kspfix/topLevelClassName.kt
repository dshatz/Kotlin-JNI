package com.dshatz.kni.kspfix

import com.dshatz.kni.kspfix.FunLocation.FunctionParent
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.toClassName

@Suppress("DefaultLocale")
private fun KSFunctionDeclaration.topLevelFunLocation(): FunLocation? {
    val fileName = containingFile?.fileName
    return fileName?.replace(".kt", "")?.substringBeforeLast('.')?.let { cls ->
        val clsKt = cls.capitalize() + "Kt"
        val pkg = packageName.asString()
        val classname = ClassName(pkg, cls)
        FunLocation(
            FunctionParent.TopLevel(containingFile!!),
            classNameKt = ClassName(pkg,clsKt),
            className = classname,
        )
    }
}

@OptIn(KspExperimental::class)
fun KSFunctionDeclaration.functionLocation(): FunLocation {
    return parentDeclaration?.closestClassDeclaration()?.innerFunLocation()
        ?: topLevelFunLocation()
        ?: error("Could not derive classname")
}

fun KSClassDeclaration.innerFunLocation(): FunLocation {
    val type = when (classKind) {
        ClassKind.CLASS, ClassKind.INTERFACE -> FunctionParent.Class(this)
        ClassKind.OBJECT -> FunctionParent.Object(this)
        else -> error("Unsupported ClassKind: ${classKind}")
    }
    val className = toClassName()
    return FunLocation(
        type,
        className,
        className
    )
}

data class FunLocation(
    val parent: FunctionParent,
    val classNameKt: ClassName,
    val className: ClassName
) {

    sealed class FunctionParent {
        abstract val declaration: KSNode
        data class Class(override val declaration: KSClassDeclaration): FunctionParent()
        data class Object(override val declaration: KSDeclaration): FunctionParent()
        data class TopLevel(override val declaration: KSFile): FunctionParent()
    }
    fun toMemberName(funName: String): MemberName {
        return if (parent is FunctionParent.TopLevel) MemberName(className.packageName, funName)
        else MemberName(className, funName)
    }
}