package com.dshatz.kni.callable

import com.dshatz.kni.TypeMapper
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

fun KSFunctionDeclaration.getSignature(typeMapper: TypeMapper): String {
    val parameterDescriptors = parameters.joinToString("") { parameter ->
        typeMapper.mapType(parameter.type).jniType.jvmType.toJniDescriptor()
    }

    val returnDescriptor = returnType?.let { typeMapper.mapType(it) }?.jniType?.jvmType?.toJniDescriptor() ?: "V"

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