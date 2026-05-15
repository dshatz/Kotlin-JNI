package com.dshatz.kni

import com.dshatz.kni.kspfix.KSFun
import com.dshatz.kni.serialization.IncludedSerializers
import com.dshatz.kni.serialization.SerializerProcessor.SerialClass
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

class Registry {
    val serializers: MutableMap<TypeName, ClassName> = mutableMapOf()

    val genericSerializers: MutableSet<IncludedSerializers.Serializer.Generic> = mutableSetOf()

    val callbacks: MutableSet<ClassName> = mutableSetOf()

    val nativeInstances: MutableSet<ClassName> = mutableSetOf()

    val declarations: MutableSet<KSFun> = mutableSetOf()

    fun serializersToString(): String {
        val list = serializers.entries.joinToString("\n") {
            "${it.key} -> ${it.value.simpleName}"
        }.takeUnless { it.isEmpty() } ?: "No serializers."
        return buildString {
            appendLine()
            appendLine("Registered serializers: ")
            append(list)
        }
    }
}