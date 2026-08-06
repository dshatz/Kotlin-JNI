package com.dshatz.kni.model

import com.dshatz.kni.utils.withSuffix
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName

data class KSCallback(
    val type: ClassName,
    val funs: List<KSCallbackFun>,
    val dependency: KSFile
) {
    fun jvmAdapterName(): ClassName {
        return type.withSuffix("_JvmAdapter")
    }
}