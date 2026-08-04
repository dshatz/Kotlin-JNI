package com.dshatz.kni

import com.squareup.kotlinpoet.MemberName

internal object CNameUtils {

    private const val JNI_PREFIX = "Java"
    private const val JNI_SEPARATOR = '_'

    internal fun jniFunctionCName(
        packageName: String,
        className: String?,
        functionName: String
    ): String = buildString {
        append(JNI_PREFIX)
        append(JNI_SEPARATOR)

        val escapedPackage = escapePart(packageName)
        if (escapedPackage.isNotBlank()) {
            append(escapedPackage)
            if (!escapedPackage.endsWith(JNI_SEPARATOR)) {
                append(JNI_SEPARATOR)
            }
        }

        val escapedClass = className?.let(::escapePart)
        if (!escapedClass.isNullOrBlank()) {
            append(escapedClass)
            if (!escapedClass.endsWith(JNI_SEPARATOR)) {
                append(JNI_SEPARATOR)
            }
        }

        append(escapePart(functionName))
    }

    /**
     * Escape (package/class/method) name(s) to make it resolvable.
     *
     * https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/design.html#resolving_native_method_names
     */
    internal fun escapePart(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '_' -> append("_1")
                '/', '.' -> append("_")
                ';' -> append("_2")
                '[' -> append("_3")
                in '\u0000'..'\u007F' -> append(char) // ASCII
                else -> {
                    // Unicode

                    val hex = char.code.toString(16).lowercase().padStart(4, '0')
                    append("_0$hex")
                }
            }
        }
    }

    fun MemberName.cname(): String {
        return jniFunctionCName(packageName, enclosingClassName?.simpleName, simpleName)
    }

    fun MemberName.cnameFunName(): String {
        return cname()
    }
}