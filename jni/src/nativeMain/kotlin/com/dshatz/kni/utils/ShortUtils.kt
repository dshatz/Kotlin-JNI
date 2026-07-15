package com.dshatz.kni.utils

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.RequiresRelease
import com.dshatz.kni.binding.jbooleanVar
import com.dshatz.kni.binding.jshortArray
import com.dshatz.kni.binding.jshortVar
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
@RequiresRelease
fun jshortArray.getShortElements(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): CPointer<jshortVar> {
    val method = env.pointed.pointedCommon?.GetShortArrayElements ?: throw JniUnavailableError()
    return method.invoke(env, this, isCopy) ?: throw OutOfMemoryError("Failed to call GetShortArrayElements")
}

@OptIn(ExperimentalForeignApi::class)
fun jshortArray.releaseElements(env: CPointer<JNIEnvVar>, elements: CPointer<jshortVar>, mode: jint = 0) {
    val method = env.pointed.pointedCommon?.ReleaseShortArrayElements
    method?.invoke(env, this, elements, mode)
}

@OptIn(ExperimentalForeignApi::class, RequiresRelease::class)
fun jshortArray.toKShortArray(env: CPointer<JNIEnvVar>): ShortArray {
    val length = this.getLength(env)
    val elements = this.getShortElements(env)

    val result = memScoped {
        ShortArray(length) {
            elements[it]
        }
    }

    this.releaseElements(env, elements)
    return result
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.newShortArray(size: jsize): jshortArray? {
    val method = pointed.pointedCommon?.NewShortArray ?: return null
    return method.invoke(this, size)
}

@OptIn(ExperimentalForeignApi::class)
fun jshortArray.fill(env: CPointer<JNIEnvVar>, value: ShortArray): jshortArray? {
    val method = env.pointed.pointedCommon?.SetShortArrayRegion ?: return null
    value.usePinned {
        val pointer = it.addressOf(0)
        method.invoke(env, this, 0, value.size, pointer)
    }
    return this
}

@OptIn(ExperimentalForeignApi::class)
fun ShortArray.toJShortArray(env: CPointer<JNIEnvVar>): jshortArray? {
    return env.newShortArray(size)?.fill(env, this)
}
