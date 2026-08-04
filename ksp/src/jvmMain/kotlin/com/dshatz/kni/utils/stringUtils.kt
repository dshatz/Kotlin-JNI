package com.dshatz.kni.utils

import java.util.Locale.getDefault

fun String.capitalized(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }