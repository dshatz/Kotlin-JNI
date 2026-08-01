package com.dshatz.kni.model

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName

data class KSClass(
    val constructors: List<KSConstructor>,
    val type: ClassName,
    val declaration: KSClassDeclaration
)