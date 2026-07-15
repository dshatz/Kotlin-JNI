package com.dshatz.kni.utils

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.RequiresRelease
import com.dshatz.kni.binding.jbooleanVar
import com.dshatz.kni.binding.jlongArray
import com.dshatz.kni.binding.jlongVar
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
fun jlongArray.getLongElements(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): CPointer<jlongVar> {
    val method = env.pointed.pointedCommon?.GetLongArrayElements ?: throw JniUnavailableError()
    return method.invoke(env, this, isCopy) ?: throw OutOfMemoryError("Failed to call GetLongArrayElements")
}

@OptIn(ExperimentalForeignApi::class)
fun jlongArray.releaseElements(env: CPointer<JNIEnvVar>, elements: CPointer<jlongVar>, mode: jint = 0) {
    val method = env.pointed.pointedCommon?.ReleaseLongArrayElements
    method?.invoke(env, this, elements, mode)
}

@OptIn(ExperimentalForeignApi::class, RequiresRelease::class)
fun jlongArray.toKLongArray(env: CPointer<JNIEnvVar>): LongArray {
    val length = this.getLength(env)
    val elements = this.getLongElements(env)

    val result = memScoped {
        LongArray(length) {
            elements[it]
        }
    }

    this.releaseElements(env, elements)
    return result
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.newLongArray(size: jsize): jlongArray? {
    val method = pointed.pointedCommon?.NewLongArray ?: return null
    return method.invoke(this, size)
}

@OptIn(ExperimentalForeignApi::class)
fun jlongArray.fill(env: CPointer<JNIEnvVar>, value: LongArray): jlongArray? {
    val method = env.pointed.pointedCommon?.SetLongArrayRegion ?: return null
    value.usePinned {
        val pointer = it.addressOf(0)
        method.invoke(env, this, 0, value.size, pointer)
    }
    return this
}

@OptIn(ExperimentalForeignApi::class)
fun LongArray.toJLongArray(env: CPointer<JNIEnvVar>): jlongArray? {
    return env.newLongArray(size)?.fill(env, this)
}