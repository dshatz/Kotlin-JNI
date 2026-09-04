package com.dshatz.kni

import com.dshatz.kni.model.KSCallback
import com.dshatz.kni.model.KSDefinedSerializer
import com.dshatz.kni.model.KSInstance
import com.dshatz.kni.model.KSJniCall
import com.dshatz.kni.model.KSWrapper
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
        return typeName in callbacks || typeName in callbackClasses
    }

    val callbackClasses: MutableSet<ClassName> = mutableSetOf()

    val nativeInstances: MutableMap<ClassName, KSInstance> = mutableMapOf()
    val nativeInstanceClasses: MutableSet<ClassName> = mutableSetOf()

    val jniCallSuspendAdapters: MutableSet<KSCallback> = mutableSetOf()
    val callbackSuspendAdapters: MutableSet<KSInstance> = mutableSetOf()

    val jniAdapters: MutableSet<TypeName> = mutableSetOf()
    val jniAdapterTypes: MutableMap<ClassName, KSWrapper> = mutableMapOf()

    val allTypes: MutableSet<TypeInfo> = mutableSetOf<TypeInfo>()

    enum class Platform {
        COMMON,
        NATIVE,
        JVM
    }

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

    fun nativeInstancesToString(): String = """
        Registered native instances:
        ${nativeInstances.keys.joinToString("\n")}
    """.trimIndent()

    fun jniAdaptersToString(): String = """
        Registered JniAdapters: 
        $jniAdapterTypes
    """.trimIndent()
}

typealias PlatformSet = Set<String>