package com.dshatz.kni.utils

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.RequiresRelease
import com.dshatz.kni.binding.jbooleanVar
import com.dshatz.kni.binding.jchar
import com.dshatz.kni.binding.jcharArray
import com.dshatz.kni.binding.jcharVar
import com.dshatz.kni.binding.jint
import com.dshatz.kni.binding.jsize
import com.dshatz.kni.error.JniUnavailableError
import com.dshatz.kni.pointedCommon
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
fun jchar.toKChar(): Char {
    return Char(this)
}

/**
 * To match signature of all other toK* methods for easier generation.
 */
@OptIn(ExperimentalForeignApi::class)
fun jchar.toKChar(env: CPointer<JNIEnvVar>): Char {
    return toKChar()
}

fun Char.toJChar(): jchar {
    return this.code.toUShort()
}

@OptIn(ExperimentalForeignApi::class)
@RequiresRelease
fun jcharArray.getCharElements(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): CPointer<jcharVar> {
    val method = env.pointed.pointedCommon?.GetCharArrayElements ?: throw JniUnavailableError()
    return method.invoke(env, this, isCopy) ?: throw OutOfMemoryError("Failed to call GetCharArrayElements")
}

@OptIn(ExperimentalForeignApi::class)
fun jcharArray.releaseElements(env: CPointer<JNIEnvVar>, elements: CPointer<jcharVar>, mode: jint = 0) {
    val method = env.pointed.pointedCommon?.ReleaseCharArrayElements
    method?.invoke(env, this, elements, mode)
}

@OptIn(ExperimentalForeignApi::class, RequiresRelease::class)
fun jcharArray.toKCharArray(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): CharArray {
    val length = this.getLength(env)
    val elements = this.getCharElements(env, isCopy)

    val result = memScoped {
        CharArray(length) {
            elements[it].toKChar()
        }
    }

    this.releaseElements(env, elements)
    return result
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.newCharArray(size: jsize): jcharArray {
    val method = pointed.pointedCommon?.NewCharArray ?: throw JniUnavailableError()
    return method.invoke(this, size) ?: throw OutOfMemoryError("Failed to call NewCharArray")
}

@OptIn(ExperimentalForeignApi::class)
fun jcharArray.fill(env: CPointer<JNIEnvVar>, value: CharArray): jcharArray {
    val method = env.pointed.pointedCommon?.SetCharArrayRegion ?: throw JniUnavailableError()
    val charArray = UShortArray(value.size) {
        value[it].toJChar()
    }
    charArray.usePinned {
        val pointer = it.addressOf(0)
        method.invoke(env, this, 0, value.size, pointer)
    }
    return this
}

@OptIn(ExperimentalForeignApi::class)
fun CharArray.toJCharArray(env: CPointer<JNIEnvVar>): jcharArray {
    return env.newCharArray(size).fill(env, this)
}