package com.dshatz.kni.model

import com.squareup.kotlinpoet.ClassName

data class KSCallback(
    val type: ClassName,
    val funs: List<KSFun>
) {
    fun jvmAdapterName(): ClassName {
        return ClassName(type.packageName, type.simpleName + "Adapter")
    }

    fun jniClassName(): String {
        return type.canonicalName.replace('.', '/')
    }
}