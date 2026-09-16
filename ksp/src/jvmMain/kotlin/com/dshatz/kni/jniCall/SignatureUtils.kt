package com.dshatz.kni.jniCall

import com.dshatz.kni.Types
import com.dshatz.kni.Types.typeOf
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName

internal fun TypeName.toJniDescriptor(): String {
    return if (this is ParameterizedTypeName) {
        if (this.rawType typeOf Types.KArray) {
            "[" + typeArguments.first().toJniDescriptor()
        } else {
            error("Cannot create JniDescriptor for generic type $this")
        }
    } else if (this is WildcardTypeName) {
        this.outTypes.first().toJniDescriptor()
    } else {
        (this as ClassName).toJniDescriptor()
    }
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