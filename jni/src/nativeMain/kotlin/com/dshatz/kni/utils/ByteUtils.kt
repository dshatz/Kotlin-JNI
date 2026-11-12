package com.dshatz.kni.utils

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.RequiresRelease
import com.dshatz.kni.binding.jbooleanVar
import com.dshatz.kni.binding.jbyteArray
import com.dshatz.kni.binding.jbyteVar
import com.dshatz.kni.binding.jint
import com.dshatz.kni.binding.jsize
import com.dshatz.kni.error.JniUnavailableError
import com.dshatz.kni.pointedCommon
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
@RequiresRelease
fun jbyteArray.getByteElements(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): CPointer<ByteVar>? {
    val method = env.pointed.pointedCommon?.GetByteArrayElements ?: throw JniUnavailableError()
    return method.invoke(env, this, isCopy)
}

@OptIn(ExperimentalForeignApi::class)
fun jbyteArray.releaseElements(env: CPointer<JNIEnvVar>, elements: CPointer<jbyteVar>, mode: jint = 0) {
    val method = env.pointed.pointedCommon?.ReleaseByteArrayElements
    method?.invoke(env, this, elements, mode)
}

@OptIn(ExperimentalForeignApi::class, RequiresRelease::class)
fun jbyteArray.toKByteArray(env: CPointer<JNIEnvVar>): ByteArray {
    val length = this.getLength(env)
    val result = ByteArray(length)
    if (length == 0) return result // addressOf(0) fails on an empty array
    val getRegion = env.pointed.pointedCommon?.GetByteArrayRegion ?: throw JniUnavailableError()
    // Copy straight into the Kotlin array: one copy, and the JVM array is never pinned or copied back.
    result.usePinned {
        getRegion.invoke(env, this, 0, length, it.addressOf(0).reinterpret())
    }
    return result
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.newByteArray(size: jsize): jbyteArray {
    val method = pointed.pointedCommon?.NewByteArray ?: throw JniUnavailableError()
    return method.invoke(this, size) ?: throw OutOfMemoryError("Failed to allocate JVM byte array of size $size")
}

@OptIn(ExperimentalForeignApi::class)
fun jbyteArray.fill(env: CPointer<JNIEnvVar>, value: ByteArray): jbyteArray {
    val method = env.pointed.pointedCommon?.SetByteArrayRegion ?: throw JniUnavailableError()
    value.usePinned {
        val pointer = it.addressOf(0)
        method.invoke(env, this, 0, value.size, pointer)
    }
    return this
}

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toJByteArray(env: CPointer<JNIEnvVar>): jbyteArray {
    return env.newByteArray(size).fill(env, this)
}