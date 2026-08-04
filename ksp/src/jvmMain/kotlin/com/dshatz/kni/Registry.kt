package com.dshatz.kni

import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.model.KSCallback
import com.dshatz.kni.model.KSDefinedSerializer
import com.dshatz.kni.model.KSJniCall
import com.dshatz.kni.model.flow.KSFlowProp
import com.dshatz.kni.serialization.IncludedSerializers
import com.dshatz.kni.serialization.SerializerProcessor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

class Registry {
    val jniCalls: MutableSet<KSJniCall> = mutableSetOf()

    val serializers: MutableMap<TypeName, KSDefinedSerializer> = mutableMapOf()
    val generatedSerializers: MutableSet<SerializerProcessor.SerialClass> = mutableSetOf()

    val genericSerializers: MutableSet<IncludedSerializers.Serializer.Generic> = mutableSetOf()
    val callbacks: MutableMap<TypeName, KSCallback> = mutableMapOf()

    fun isCallback(typeName: TypeName): Boolean {
        return typeName in callbacks
    }

    val nativeInstances: MutableSet<ClassName> = mutableSetOf()

    fun serializersToString(): String {
        val list = serializers.values.joinToString("\n") {
            "${it.typeName} -> ${it.serializer.simpleName}"
        }.takeUnless { it.isEmpty() } ?: "No serializers."
        return buildString {
            appendLine()
            appendLine("Registered serializers: ")
            append(list)
        }
    }

    val flowFields: MutableMap<FunctionParent.Class, List<KSFlowProp>> = mutableMapOf()

    fun clear() {
        jniCalls.clear()
        generatedSerializers.clear()
        genericSerializers.clear()
        callbacks.clear()
        nativeInstances.clear()
        flowFields.clear()
    }
}