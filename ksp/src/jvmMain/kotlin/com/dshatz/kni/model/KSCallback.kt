package com.dshatz.kni.model

import com.dshatz.kni.utils.withSuffix
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

data class KSCallback(
    val type: ClassName,
    val funs: List<KSCallbackFun>,
    val dependency: KSFile,
    val superType: TypeName?,
) {
    fun jvmAdapterName(): ClassName {
        return type.withSuffix("_JvmAdapter")
    }
}