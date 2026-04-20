package com.dshatz.kni.kspfix

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.toClassName

private fun KSFunctionDeclaration.topLevelClassName(): FunLocation? {
    val fileName = containingFile?.fileName
    return fileName?.replace(".kt", "")?.substringBeforeLast('.')?.let { cls ->
        val clsKt = cls + "Kt"
        val pkg = packageName.asString()
        val classname = ClassName(pkg, cls)
        FunLocation(
            true,
            classNameKt = ClassName(pkg,clsKt),
            className = classname,
        ).also {
        }
    }
}

@OptIn(KspExperimental::class)
fun KSFunctionDeclaration.functionLocation(): FunLocation {
    return parentDeclaration?.closestClassDeclaration()?.toClassName()?.let { FunLocation(false, it, it) }
        ?: topLevelClassName()
        ?: error("Could not derive classname")
}

data class FunLocation(
    val topLevel: Boolean,
    val classNameKt: ClassName,
    val className: ClassName,
) {
    fun toMemberName(funName: String): MemberName {
        return if (topLevel) MemberName(className.packageName, funName)
        else MemberName(className, funName)
    }
}

data class KSFun(
    val f: KSFunctionDeclaration,
    val location: FunLocation,
)

fun List<KSFunctionDeclaration>.withLocations(): List<KSFun> {
    return map {
        KSFun(it, it.functionLocation())
    }
}