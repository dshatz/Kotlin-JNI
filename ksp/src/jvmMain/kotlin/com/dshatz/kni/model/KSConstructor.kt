package com.dshatz.kni.model

import com.squareup.kotlinpoet.KModifier

data class KSConstructor(
    val id: Int,
    val params: List<ParamInfo>,
    val modifier: KModifier
)