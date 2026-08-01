package com.dshatz.kni

import com.dshatz.kni.model.KSCallback
import com.dshatz.kni.model.KSFun
import com.dshatz.kni.serialization.IncludedSerializers
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

class Registry {
    val serializers: MutableMap<TypeName, ClassName> = mutableMapOf()

    val genericSerializers: MutableSet<IncludedSerializers.Serializer.Generic> = mutableSetOf()

    private val callbacks: MutableSet<KSCallback> = mutableSetOf()
    private val callbackClasses: MutableSet<TypeName> = mutableSetOf()
    fun addCallbacks(callbacks: List<KSCallback>) {
        this.callbacks.addAll(callbacks)
        this.callbackClasses.addAll(callbacks.map { it.type })
    }

    fun isCallback(typeName: TypeName): Boolean {
        return typeName in callbackClasses
    }

    val nativeInstances: MutableSet<ClassName> = mutableSetOf()

    val callables: MutableSet<KSFun> = mutableSetOf()

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