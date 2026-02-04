package dev.datlag.nkommons.utils

import dev.datlag.nkommons.binding.ByteBuffer
import dev.datlag.nkommons.JNIEnvVar
import dev.datlag.nkommons.binding.*
import dev.datlag.nkommons.jvalue
import dev.datlag.nkommons.l
import dev.datlag.nkommons.pointedCommon
import kotlinx.cinterop.*


/**
 * Convert jobject representing a java.nio.ByteBuffer to a native [binding.ByteBuffer] wrapper.
 *
 * @receiver a java.nio.ByteBuffer as received into a native function.
 */
@OptIn(ExperimentalForeignApi::class)
fun jobject.toKDirectByteBuffer(env: CPointer<JNIEnvVar>): ByteBuffer {
    val rawAddress = env.pointed.pointedCommon!!.GetDirectBufferAddress!!.invoke(env, this)
    val size = env.pointed.pointedCommon!!.GetDirectBufferCapacity!!.invoke(env, this)
    val address = rawAddress!!.reinterpret<ByteVar>()
    return ByteBuffer(address, size)
}


/**
 * Convert a native wrapper [binding.ByteBuffer] to a jobject representing the same [binding.ByteBuffer].
 *
 * @return a jobject representing a [binding.ByteBuffer] or null if operation failed.
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

/**
 * Since JDK 1.2, when FindClass is called through the Invocation Interface, there is no current native method or its associated class loader. In that case, the result of ClassLoader.getSystemClassLoader is used. This is the class loader the virtual machine creates for applications, and is able to locate classes listed in the java.class.path property.
 *
 * *See also:* [JNI Reference](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html).
 *
 * @param name a fully-qualified class name (that is, a package name, delimited by “/”, followed by the class name). If the name begins with “[“ (the array signature character), it returns an array class. The string is encoded in modified UTF-8.
 * @return a class object from a fully-qualified name, or NULL if the class cannot be found.
 *
 */
@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.FindClass(name: String): jclass? = memScoped {
    pointed.pointedCommon!!.FindClass!!.invoke(this@FindClass, name.cstr.ptr)
}

/**
 * Creates a new global reference to the object referred to by the [obj] argument. The obj argument may be a global or local reference. Global references must be explicitly disposed of by calling [DeleteGlobalRef].
 *
 * *See also:* [JNI Reference](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html).
 *
 * @param obj a global or local reference. May be a NULL value, in which case this function will return NULL.
 * @return a global reference to the given obj.
 */
@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.NewGlobalRef(obj: jobject?): jobject? {
    return pointed.pointedCommon!!.NewGlobalRef!!.invoke(this, obj)
}

/**
 * Deletes the global reference pointed to by [globalRef].
 *
 * *See also:* [JNI Reference](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html).
 *
 * @see [NewGlobalRef].
 */
@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.DeleteGlobalRef(globalRef: jobject) {
    pointed.pointedCommon!!.DeleteGlobalRef!!.invoke(this, globalRef)
}

/**
 * Deletes the local reference pointed to by localRef.
 *
 * *See also:* [JNI Reference](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html).
 * @param localRef a local reference. The function does nothing in the case of a NULL value passed here.
 */
@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.DeleteLocalRef(localRef: jobject?) {
    pointed.pointedCommon!!.DeleteLocalRef!!.invoke(this, localRef)
}

/**
 * Returns the method ID for a static method of a class. The method is specified by its name and signature.
 *
 * *See also:* [JNI Reference](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html).
 *
 * @return a method ID, or NULL if the operation fails.
 */
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

/**
 * Returns the method ID for an instance (nonstatic) method of a class or interface. The method may be defined in one of the clazz’s supertypes and inherited by clazz. The method is determined by its name and signature.
 *
 * GetMethodID() causes an uninitialized class to be initialized.
 *
 * To obtain the method ID of a constructor, supply <init> as the method name and void (V) as the return type.
 *
 * *See also:* [JNI Reference](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html).
 *
 * @return a method ID, or NULL if the specified method cannot be found.
 */
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

/**
 * Returns the field ID for an instance (nonstatic) field of a class. The field is specified by its name and signature.
 *
 * *See also:* [JNI Reference](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html).
 *
 * @return a field ID, or NULL if the operation fails.
 */
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