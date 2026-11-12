package com.dshatz.kni.utils

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.RequiresRelease
import com.dshatz.kni.binding.jbooleanVar
import com.dshatz.kni.binding.jdoubleArray
import com.dshatz.kni.binding.jdoubleVar
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
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
@RequiresRelease
fun jdoubleArray.getDoubleElements(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): CPointer<jdoubleVar> {
    val method = env.pointed.pointedCommon?.GetDoubleArrayElements ?: throw JniUnavailableError()
    return method.invoke(env, this, isCopy) ?: throw OutOfMemoryError("Failed to call GetDoubleArrayElements")
}

@OptIn(ExperimentalForeignApi::class)
fun jdoubleArray.releaseElements(env: CPointer<JNIEnvVar>, elements: CPointer<jdoubleVar>, mode: jint = 0) {
    val method = env.pointed.pointedCommon?.ReleaseDoubleArrayElements
    method?.invoke(env, this, elements, mode)
}

@OptIn(ExperimentalForeignApi::class, RequiresRelease::class)
fun jdoubleArray.toKDoubleArray(env: CPointer<JNIEnvVar>): DoubleArray {
    val length = this.getLength(env)
    val result = DoubleArray(length)
    if (length == 0) return result // addressOf(0) fails on an empty array
    val getRegion = env.pointed.pointedCommon?.GetDoubleArrayRegion ?: throw JniUnavailableError()
    // Copy straight into the Kotlin array: one copy, and the JVM array is never pinned or copied back.
    result.usePinned {
        getRegion.invoke(env, this, 0, length, it.addressOf(0).reinterpret())
    }
    return result
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.newDoubleArray(size: jsize): jdoubleArray {
    val method = pointed.pointedCommon?.NewDoubleArray ?: throw JniUnavailableError()
    return method.invoke(this, size) ?: throw OutOfMemoryError("Failed to call NewDoubleArray")
}

@OptIn(ExperimentalForeignApi::class)
fun jdoubleArray.fill(env: CPointer<JNIEnvVar>, value: DoubleArray): jdoubleArray {
    if (value.isEmpty()) return this // addressOf(0) fails on an empty array
    val method = env.pointed.pointedCommon?.SetDoubleArrayRegion ?: throw JniUnavailableError()
    value.usePinned {
        val pointer = it.addressOf(0)
        method.invoke(env, this, 0, value.size, pointer)
    }
    return this
}

@OptIn(ExperimentalForeignApi::class)
fun DoubleArray.toJDoubleArray(env: CPointer<JNIEnvVar>): jdoubleArray {
    return env.newDoubleArray(size).fill(env, this)
}