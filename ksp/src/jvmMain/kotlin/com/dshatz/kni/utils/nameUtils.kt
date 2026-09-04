package com.dshatz.kni.utils

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName

fun ParameterizedTypeName.genericClassName(): String {
    return runCatching {
        "${rawType.simpleName}_Of_${typeArguments.joinToString("_") { it.safeQualifiedName().replace('.', '_') }}"
    }.onFailure {
        println("Failed to get genericClassName for $this")
        throw it
    }.getOrThrow()
}

fun TypeName.safeQualifiedName(): String {
    return when (this) {
        is ClassName -> this.canonicalName
        is ParameterizedTypeName -> this.rawType.packageName + "." + genericClassName()
        is WildcardTypeName -> (inTypes + outTypes).first().safeQualifiedName()
        else -> error("Could not make a name for $this")
    }
}