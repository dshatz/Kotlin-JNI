package com.dshatz.kni.utils

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.RequiresRelease
import com.dshatz.kni.binding.jbooleanVar
import com.dshatz.kni.binding.jint
import com.dshatz.kni.binding.jintArray
import com.dshatz.kni.binding.jintVar
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
fun jintArray.getIntElements(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): CPointer<jintVar> {
    val method = env.pointed.pointedCommon?.GetIntArrayElements ?: throw JniUnavailableError()
    return method.invoke(env, this, isCopy) ?: throw OutOfMemoryError("Failed to call GetIntArrayElements")
}

@OptIn(ExperimentalForeignApi::class)
fun jintArray.releaseElements(env: CPointer<JNIEnvVar>, elements: CPointer<jintVar>, mode: jint = 0) {
    val method = env.pointed.pointedCommon?.ReleaseIntArrayElements ?: throw JniUnavailableError()
    method.invoke(env, this, elements, mode)
}

@OptIn(ExperimentalForeignApi::class, RequiresRelease::class)
fun jintArray.toKIntArray(env: CPointer<JNIEnvVar>): IntArray {
    val length = this.getLength(env)
    val result = IntArray(length)
    if (length == 0) return result // addressOf(0) fails on an empty array
    val getRegion = env.pointed.pointedCommon?.GetIntArrayRegion ?: throw JniUnavailableError()
    // Copy straight into the Kotlin array: one copy, and the JVM array is never pinned or copied back.
    result.usePinned {
        getRegion.invoke(env, this, 0, length, it.addressOf(0).reinterpret())
    }
    return result
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.newIntArray(size: jsize): jintArray {
    val method = pointed.pointedCommon?.NewIntArray ?: throw JniUnavailableError()
    return method.invoke(this, size) ?: throw OutOfMemoryError("Failed to allocate JVM int array of size $size")
}

@OptIn(ExperimentalForeignApi::class)
fun jintArray.fill(env: CPointer<JNIEnvVar>, value: IntArray): jintArray {
    val method = env.pointed.pointedCommon?.SetIntArrayRegion ?: throw JniUnavailableError()
    value.usePinned { pinnedArray ->
        val pointer = pinnedArray.addressOf(0)
        method.invoke(env, this, 0, value.size, pointer)
    }
    return this
}

@OptIn(ExperimentalForeignApi::class)
fun IntArray.toJIntArray(env: CPointer<JNIEnvVar>): jintArray {
    return env.newIntArray(size).fill(env, this)
}
