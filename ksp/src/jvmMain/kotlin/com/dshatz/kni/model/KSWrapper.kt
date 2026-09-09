package com.dshatz.kni.model

import com.dshatz.kni.TypeInfo
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

data class KSWrapper(
    val adapterCls: ClassName,
    val inner: TypeName,
    val type: TypeInfo.JniAdapter
)