package com.dshatz.kni.model

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName

data class KSCallback(
    val type: ClassName,
    val declaration: KSClassDeclaration,
    val funs: List<KSFun>
) {
    fun jvmAdapterName(): ClassName {
        return ClassName(type.packageName, type.simpleName + "Adapter")
    }
}