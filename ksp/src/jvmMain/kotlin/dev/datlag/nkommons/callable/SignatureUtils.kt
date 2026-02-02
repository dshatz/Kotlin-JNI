package dev.datlag.nkommons.callable

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import dev.datlag.nkommons.TypeMatcher

fun KSFunctionDeclaration.getSignature(): String {
    val parameterDescriptors = parameters.joinToString("") { parameter ->
        parameter.type.resolve().toJniDescriptor()
    }

    val returnDescriptor = returnType?.resolve()?.toJniDescriptor() ?: "V"

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
    return when (this.toClassName()) {
        TypeMatcher.KString -> {
            "l" to CodeBlock.of(".%M(env)?.%M()", TypeMatcher.Method.ToJString, Def.reinterpret)
        }

        TypeMatcher.KByteArray -> {
            "l" to CodeBlock.of(".%M(env)?.%M()", TypeMatcher.Method.ToJByteArray, Def.reinterpret)
        }

        TypeMatcher.KByteBuffer -> {
            "l" to CodeBlock.of(".%M(env)?.%M()", TypeMatcher.Method.ToJByteBuffer, Def.reinterpret)
        }
        else -> {
            when (this.toClassName()) {
                TypeMatcher.KBoolean -> "z"
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