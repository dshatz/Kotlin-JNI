package com.dshatz.kni.utils

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.RequiresRelease
import com.dshatz.kni.binding.jbooleanVar
import com.dshatz.kni.binding.jdoubleArray
import com.dshatz.kni.binding.jdoubleVar
import com.dshatz.kni.binding.jint
import com.dshatz.kni.binding.jsize
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
@RequiresRelease
fun jdoubleArray.getDoubleElements(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): CPointer<jdoubleVar>? {
    val method = env.pointed.pointedCommon?.GetDoubleArrayElements ?: return null
    return method.invoke(env, this, isCopy)
}

@OptIn(ExperimentalForeignApi::class)
fun jdoubleArray.releaseElements(env: CPointer<JNIEnvVar>, elements: CPointer<jdoubleVar>, mode: jint = 0) {
    val method = env.pointed.pointedCommon?.ReleaseDoubleArrayElements
    method?.invoke(env, this, elements, mode)
}

@OptIn(ExperimentalForeignApi::class, RequiresRelease::class)
fun jdoubleArray.toKDoubleArray(env: CPointer<JNIEnvVar>): DoubleArray? {
    val length = this.getLength(env) ?: return null
    val elements = this.getDoubleElements(env) ?: return null

    val result = memScoped {
        DoubleArray(length) {
            elements[it]
        }
    }

    this.releaseElements(env, elements)
    return result
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.newDoubleArray(size: jsize): jdoubleArray? {
    val method = pointed.pointedCommon?.NewDoubleArray ?: return null
    return method.invoke(this, size)
}

@OptIn(ExperimentalForeignApi::class)
fun jdoubleArray.fill(env: CPointer<JNIEnvVar>, value: DoubleArray): jdoubleArray? {
    val method = env.pointed.pointedCommon?.SetDoubleArrayRegion ?: return null
    value.usePinned {
        val pointer = it.addressOf(0)
        method.invoke(env, this, 0, value.size, pointer)
    }
    return this
}

@OptIn(ExperimentalForeignApi::class)
fun DoubleArray.toJDoubleArray(env: CPointer<JNIEnvVar>): jdoubleArray? {
    return env.newDoubleArray(size)?.fill(env, this)
}