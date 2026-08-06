package com.dshatz.kni.utils

import com.squareup.kotlinpoet.ClassName
import java.util.Locale.getDefault

fun String.capitalized(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }

fun ClassName.withSuffix(suffix: String): ClassName {
    return ClassName(packageName, simpleName + suffix)
}

fun ClassName.jniClassName(): String {
    return canonicalName.replace('.', '/')
}