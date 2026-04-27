package com.dshatz.kni

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

class Registry {
    val serializers: MutableMap<TypeName, ClassName> = mutableMapOf()
    val callables: MutableSet<ClassName> = mutableSetOf()

    val nativeInstances: MutableSet<ClassName> = mutableSetOf()
}