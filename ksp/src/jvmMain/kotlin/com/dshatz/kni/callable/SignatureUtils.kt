package com.dshatz.kni.callable

import com.dshatz.kni.TypeMatcher
import com.dshatz.kni.utils.dereferenceTypeAlias
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName

fun KSFunctionDeclaration.getSignature(): String {
    val parameterDescriptors = parameters.joinToString("") { parameter ->
        parameter.type.dereferenceTypeAlias().toJniDescriptor()
    }

    val returnDescriptor = returnType?.dereferenceTypeAlias()?.toJniDescriptor() ?: "V"

    return "($parameterDescriptors)$returnDescriptor"
}

private fun KSType.toJniDescriptor(): String {
    val declaration = this.declaration
    val qualifiedName = declaration.qualifiedName?.asString() ?: return "Ljava/lang/Object;"

    return when (qualifiedName) {
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
            "L${qualifiedName.replace('.', '/')};"
        }
    }
}

fun KSType.toJValueField(): Pair<String, CodeBlock> {
    val cls = this.toClassName()
    val nullCheck = if (cls.isNullable) "?." else "."
    return when (this.toClassName().copy(nullable = false)) {
        TypeMatcher.KString -> {
            "l" to CodeBlock.of("%L%M(env)?.%M()", nullCheck, TypeMatcher.Method.ToJString, Def.reinterpret)
        }

        TypeMatcher.KByteArray -> {
            "l" to CodeBlock.of("%L%M(env)?.%M()", nullCheck, TypeMatcher.Method.ToJByteArray, Def.reinterpret)
        }

        TypeMatcher.KByteBuffer -> {
            "l" to CodeBlock.of("%L%M(env)?.%M()", nullCheck, TypeMatcher.Method.ToJByteBuffer, Def.reinterpret)
        }

        TypeMatcher.KBoolean -> {
            "z" to CodeBlock.of("%L%M()", nullCheck, TypeMatcher.Method.ToJBoolean)
        }
        else -> {
            when (this.toClassName()) {
                TypeMatcher.KByte    -> "b"
                TypeMatcher.KChar    -> "c"
                TypeMatcher.KShort   -> "s"
                TypeMatcher.KInt     -> "i"
                TypeMatcher.KLong    -> "j"
                TypeMatcher.KFloat   -> "f"
                TypeMatcher.KDouble  -> "d"
                else -> "l"
            } to CodeBlock.of("")
        }
    }
}