package com.dshatz.kni

import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.model.KSCallback
import com.dshatz.kni.model.KSCallbackFun
import com.dshatz.kni.model.KSDefinedSerializer
import com.dshatz.kni.model.KSJniCall
import com.dshatz.kni.model.flow.KSFlowProp
import com.dshatz.kni.serialization.IncludedSerializers
import com.dshatz.kni.serialization.SerializerProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

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

    val jniCallSuspendAdapters: MutableSet<KSCallback> = mutableSetOf()

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
        ${nativeInstances.joinToString("\n")}
    """.trimIndent()

    val flowFields: MutableMap<FunctionParent.Class, List<KSFlowProp>> = mutableMapOf()
}