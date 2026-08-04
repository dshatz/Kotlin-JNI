package com.dshatz.kni.model

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName

data class KSCallback(
    val type: ClassName,
    val funs: List<KSCallbackFun>,
    val dependency: KSFile
) {
    fun jvmAdapterName(): ClassName {
        return ClassName(type.packageName, type.simpleName + "Adapter")
    }

    fun jniClassName(): String {
        return type.canonicalName.replace('.', '/')
    }
}