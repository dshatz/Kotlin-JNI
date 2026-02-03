package dev.datlag.nkommons.utils

import dev.datlag.nkommons.binding.ByteBuffer
import dev.datlag.nkommons.JNIEnvVar
import dev.datlag.nkommons.binding.*
import dev.datlag.nkommons.jvalue
import dev.datlag.nkommons.l
import dev.datlag.nkommons.pointedCommon
import kotlinx.cinterop.*


/**
 * Convert jobject representing a java.nio.ByteBuffer to a native [ByteBuffer] wrapper.
 */
@OptIn(ExperimentalForeignApi::class)
fun jobject.toKDirectByteBuffer(env: CPointer<JNIEnvVar>): ByteBuffer {
    val rawAddress = env.pointed.pointedCommon!!.GetDirectBufferAddress!!.invoke(env, this)
    val size = env.pointed.pointedCommon!!.GetDirectBufferCapacity!!.invoke(env, this)
    val address = rawAddress!!.reinterpret<ByteVar>()
    return ByteBuffer(address, size)
}


/**
 * Convert a native wrapper [ByteBuffer] to a jobject representing the same [ByteBuffer].
 */
@OptIn(ExperimentalForeignApi::class)
fun ByteBuffer.toJByteBuffer(env: CPointer<JNIEnvVar>): jobject? {
    val jvmBuffer = env.pointed.pointedCommon!!.NewDirectByteBuffer!!(env, address, size)
    val cls = env.FindClass(ByteBuffer::class.qualifiedName!!.replace('.', '/'))!!
    val constructor = env.GetMethodID(cls, "<init>", "(Ljava/nio/ByteBuffer;)V")!!
    return memScoped {
        val args = allocArray<jvalue>(1)
        args[0].l = jvmBuffer
        env.pointed.pointedCommon!!.NewObjectA!!(env, cls, constructor, args)
    }
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.FindClass(name: String): jclass? = memScoped {
    pointed.pointedCommon!!.FindClass!!.invoke(this@FindClass, name.cstr.ptr)
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.NewGlobalRef(obj: jobject): jobject? {
    return pointed.pointedCommon!!.NewGlobalRef!!.invoke(this, obj)
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.DeleteGlobalRef(obj: jobject) {
    pointed.pointedCommon!!.DeleteGlobalRef!!.invoke(this, obj)
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.DeleteLocalRef(obj: jobject) {
    pointed.pointedCommon!!.DeleteLocalRef!!.invoke(this, obj)
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.GetStaticMethodID(
    classRef: jobject,
    methodName: String,
    signature: String
): jmethodID? = memScoped {
    pointed.pointedCommon!!.GetStaticMethodID!!.invoke(
        this@GetStaticMethodID,
        classRef,
        methodName.cstr.ptr,
        signature.cstr.ptr
    )
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.GetMethodID(
    classRef: jobject,
    methodName: String,
    signature: String
): jmethodID? = memScoped {
    pointed.pointedCommon!!.GetMethodID!!.invoke(
        this@GetMethodID,
        classRef,
        methodName.cstr.ptr,
        signature.cstr.ptr
    )
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.GetFieldID(
    classRef: jobject,
    name: String,
    returnType: String
): jmethodID? = memScoped {
    pointed.pointedCommon!!.GetFieldID!!.invoke(
        this@GetFieldID,
        classRef,
        name.cstr.ptr,
        returnType.cstr.ptr
    )
}


@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.CallIntMethodA(
    obj: jobject,
    method: jmethodID,
    args: CPointer<jvalue>
): jint {
    return pointed.pointedCommon!!.CallIntMethodA!!.invoke(
        this,
        obj,
        method,
        args
    )
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.CallObjectMethodA(
    obj: jobject,
    method: jmethodID,
    args: CPointer<jvalue>
): jobject? {
    return pointed.pointedCommon!!.CallObjectMethodA!!.invoke(
        this,
        obj,
        method,
        args
    )
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.CallDoubleMethodA(
    obj: jobject,
    method: jmethodID,
    args: CPointer<jvalue>
): jdouble? {
    return pointed.pointedCommon!!.CallDoubleMethodA!!.invoke(
        this,
        obj,
        method,
        args
    )
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.CallFloatMethodA(
    obj: jobject,
    method: jmethodID,
    args: CPointer<jvalue>
): jfloat? {
    return pointed.pointedCommon!!.CallFloatMethodA!!.invoke(
        this,
        obj,
        method,
        args
    )
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.CallByteMethodA(
    obj: jobject,
    method: jmethodID,
    args: CPointer<jvalue>
): jbyte {
    return pointed.pointedCommon!!.CallByteMethodA!!.invoke(
        this,
        obj,
        method,
        args
    )
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.CallCharMethodA(
    obj: jobject,
    method: jmethodID,
    args: CPointer<jvalue>
): jchar {
    return pointed.pointedCommon!!.CallCharMethodA!!.invoke(
        this,
        obj,
        method,
        args
    )
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.CallBooleanMethodA(
    obj: jobject,
    method: jmethodID,
    args: CPointer<jvalue>
): jboolean {
    return pointed.pointedCommon!!.CallBooleanMethodA!!.invoke(
        this,
        obj,
        method,
        args
    )
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.CallVoidMethodA(
    obj: jobject,
    method: jmethodID,
    args: CPointer<jvalue>
) {
    return pointed.pointedCommon!!.CallVoidMethodA!!.invoke(
        this,
        obj,
        method,
        args
    )
}