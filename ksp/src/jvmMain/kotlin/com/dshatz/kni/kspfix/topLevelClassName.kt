package com.dshatz.kni.kspfix

import com.dshatz.kni.TypeInfo
import com.dshatz.kni.callable.CallableProcessor
import com.dshatz.kni.kspfix.FunLocation.LocationType
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName

private fun KSFunctionDeclaration.topLevelFunLocation(): FunLocation? {
    val fileName = containingFile?.fileName
    return fileName?.replace(".kt", "")?.substringBeforeLast('.')?.let { cls ->
        val clsKt = cls + "Kt"
        val pkg = packageName.asString()
        val classname = ClassName(pkg, cls)
        FunLocation(
            LocationType.TOPLEVEL,
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
        ClassKind.CLASS -> FunLocation.LocationType.CLASS
        ClassKind.OBJECT -> FunLocation.LocationType.OBJECT
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
    val locationType: LocationType,
    val classNameKt: ClassName,
    val className: ClassName,
) {

    enum class LocationType {
        CLASS,
        OBJECT,
        TOPLEVEL
    }
    fun toMemberName(funName: String): MemberName {
        return if (locationType == LocationType.TOPLEVEL) MemberName(className.packageName, funName)
        else MemberName(className, funName)
    }
}

data class KSFun(
    val simpleName: String,
    val returnType: TypeInfo,
    val parameters: List<CallableProcessor.ParamInfo>,
    val location: FunLocation,
    val classDeclaration: KSClass?,
)

data class KSClass(
    val constructorParams: List<CallableProcessor.ParamInfo>,
    val type: ClassName
)

/*
fun List<KSFunctionDeclaration>.withLocations(): List<KSFun> {
    return map {
        KSFun(
            it.simpleName.asString(),
            it,
            it.functionLocation(),
            it.closestClassDeclaration()?.takeIf { it.classKind == ClassKind.CLASS }
        )
    }
}*/
