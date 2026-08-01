package com.dshatz.kni.model

import com.dshatz.kni.TypeInfo
import com.dshatz.kni.kspfix.FunLocation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

data class KSFun(
    val simpleName: String,
    val returnType: TypeInfo,
    val parameters: List<ParamInfo>,
    val location: FunLocation,
    val cls: KSClass?,
    val declaration: KSFunctionDeclaration
)