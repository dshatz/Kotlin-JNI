package com.dshatz.kni.callable

import com.dshatz.kni.model.KSCallFun
import com.dshatz.kni.model.KSFun
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

fun KSFun.getSignature(): String {
    val parameterDescriptors = parameters.joinToString("") { parameter ->
        parameter.typeInfo.jniType.jvmType.toJniDescriptor()
    }

    val returnDescriptor = returnType.jniType.jvmType.toJniDescriptor()

    return "($parameterDescriptors)$returnDescriptor"
}

private fun TypeName.toJniDescriptor(): String {
    return (this as ClassName).toJniDescriptor()
}

private fun ClassName.toJniDescriptor(): String {

    return when (canonicalName) {
        "kotlin.Unit" -> "V"
        "kotlin.Boolean" -> "Z"
        "kotlin.Byte" -> "B"
        "kotlin.Char" -> "C"
        "kotlin.Short" -> "S"
        "kotlin.Int" -> "I"
        "kotlin.Long" -> "J"
        "kotlin.Float" -> "F"
        "kotlin.Double" -> "D"
        "kotlin.String" -> "Ljava/lang/String;"
        "java.nio.ByteBuffer" -> "Ljava/nio/ByteBuffer;"
        "kotlin.ByteArray" -> "[B"
        "kotlin.IntArray" -> "[I"
        else -> {
            "L${canonicalName.replace('.', '/')};"
        }
    }
}