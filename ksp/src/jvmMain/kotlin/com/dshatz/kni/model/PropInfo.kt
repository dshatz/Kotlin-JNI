package com.dshatz.kni.model

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.TypeName

data class PropInfo(
    val name: String,
    val type: TypeName,
    val isMutable: Boolean,
    val declaration: KSPropertyDeclaration
)