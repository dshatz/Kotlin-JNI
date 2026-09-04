package com.dshatz.kni.utils

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.binding.jarray
import com.dshatz.kni.binding.jclass
import com.dshatz.kni.binding.jobject
import com.dshatz.kni.binding.jobjectArray
import com.dshatz.kni.error.JniUnavailableError
import com.dshatz.kni.pointedCommon
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed

@OptIn(ExperimentalForeignApi::class)
fun jarray.getLength(env: CPointer<JNIEnvVar>): Int {
    val method = env.pointed.pointedCommon?.GetArrayLength ?: throw JniUnavailableError()
    return method.invoke(env, this)
}

@OptIn(ExperimentalForeignApi::class)
fun jobjectArray.getObjectElement(env: CPointer<JNIEnvVar>, index: Int): jobject {
    val method = env.pointed.pointedCommon?.GetObjectArrayElement ?: throw JniUnavailableError()
    return method.invoke(env, this, index) ?: throw OutOfMemoryError("Failed to call GetBooleanArrayElements")
}

@OptIn(ExperimentalForeignApi::class)
inline fun <reified T> jobjectArray.toKObjectArray(
    env: CPointer<JNIEnvVar>,
    convert: jobject.() -> T
): Array<T> {
    val length = this.getLength(env)
    return Array(length) { idx ->
        convert(getObjectElement(env, idx))
    }
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.NewObjectArray(
    size: Int,
    cls: jclass,
): jobjectArray {
    val method = pointed.pointedCommon?.NewObjectArray ?: throw JniUnavailableError()
    return method.invoke(this, size, cls, null) ?: throw OutOfMemoryError("Failed to call NewObjectArray")
}

@OptIn(ExperimentalForeignApi::class)
fun jobjectArray.SetObjectArrayElement(
    env: CPointer<JNIEnvVar>,
    index: Int,
    value: jobject
) {
    val method = env.pointed.pointedCommon?.SetObjectArrayElement ?: throw JniUnavailableError()
    method.invoke(env, this, index, value)
}

@OptIn(ExperimentalForeignApi::class)
inline fun <reified T> Array<T>.toJObjectArray(
    env: CPointer<JNIEnvVar>,
    convert: T.() -> jobject
): jobjectArray {
    val cls = T::class.qualifiedName?.let { qualified ->
        env.FindClass(qualified.replace('.', '/')) ?: error("Could not find class name $qualified")
    } ?: error("Cannot convert local or anonymous class ${T::class} to jclass.")
    val arr = env.NewObjectArray(size, cls)
    forEachIndexed { index, t ->
        arr.SetObjectArrayElement(env, index, convert(t))
    }
    return arr
}