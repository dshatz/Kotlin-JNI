package com.dshatz.kni.model

import com.dshatz.kni.TypeInfo
import com.dshatz.kni.kspfix.FunctionParent
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

data class KSCallFun(
    val name: String,
    val returnType: TypeInfo,
    val parameters: List<ParamInfo>,
    override val parent: FunctionParent,
    val declaration: KSFunctionDeclaration
): WithParent

data class KSFun(
    val name: String,
    val returnType: TypeInfo,
    val parameters: List<ParamInfo>,
    override val parent: FunctionParent
): WithParent

