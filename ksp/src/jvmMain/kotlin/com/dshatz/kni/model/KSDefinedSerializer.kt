package com.dshatz.kni.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

data class KSDefinedSerializer(
    val typeName: TypeName,
    val serializer: ClassName
)