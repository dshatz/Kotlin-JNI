package com.dshatz.kni.utils

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.binding.*
import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.buffers.DelicateBufferAPI
import com.dshatz.kni.error.JniUnavailableError
import com.dshatz.kni.jvalue
import com.dshatz.kni.l
import com.dshatz.kni.pointedCommon
import kotlinx.cinterop.*
import kotlinx.cinterop.toCPointer
import platform.posix.close


/**
 * Convert jobject representing a java.nio.ByteBuffer to a native [ByteBuffer] wrapper.
 *
 * @receiver a java.nio.ByteBuffer as received into a native function.
 */
@OptIn(ExperimentalForeignApi::class, DelicateBufferAPI::class)
fun jobject.toKDirectByteBuffer(env: CPointer<JNIEnvVar>): ByteBuffer {
    val globalRef = env.NewGlobalRef(this)
    val getDirectBufferAddress = env.pointed.pointedCommon?.GetDirectBufferAddress ?: throw JniUnavailableError()
    val getDirectBufferCapacity = env.pointed.pointedCommon?.GetDirectBufferCapacity ?: throw JniUnavailableError()

    val rawAddress = getDirectBufferAddress.invoke(env, globalRef) ?: error("GetDirectBufferAddress returned null")
    val size = getDirectBufferCapacity.invoke(env, globalRef)

    val address = rawAddress.reinterpret<ByteVar>()
    return ByteBuffer.wrapAddress(address, size, owner = globalRef, finalizer = {
        it.let(env::DeleteGlobalRef)
    })
}

class JvmCallException(className: String, message: String): Exception(
    "JVM Exception: $className($message)",
    cause = RuntimeException("$className: $message")
)

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.checkException(): String? {
    val check = ExceptionCheck()
    return if (check == 1.toUByte()) {
        val jexception = ExceptionOccurred()!!
        val cls = GetObjectClass(jexception)
        val getMessage = GetMethodID(cls, "getMessage", "()Ljava/lang/String;")!!
        val messageStringObj = CallObjectMethodA(jexception, getMessage, null, noExceptionCheck = true)
            ?.toKString(this)


        val getNameMethod = GetMethodID(GetObjectClass(cls), "getName", "()Ljava/lang/String;")
        val classNameObj = getNameMethod?.let {
            CallObjectMethodA(cls, it, null, noExceptionCheck = true)
        }
        val exceptionClassName = classNameObj?.toKString(this) ?: "UnknownJvmException"
        val message = messageStringObj.orEmpty()
        ExceptionClear()
        throw JvmCallException(exceptionClassName, message)
    } else null
}


/**
 * Convert a native wrapper [ByteBuffer] to a jobject representing the same [ByteBuffer].
 *
 * @return a jobject representing a [ByteBuffer] or null if operation failed.
 */
@OptIn(ExperimentalForeignApi::class)
fun ByteBuffer.toJByteBuffer(env: CPointer<JNIEnvVar>): jobject {
    val jvmBuffer = toJNioByteBuffer(env)
    val cls = env.FindClass(ByteBuffer::class.qualifiedName!!.replace('.', '/'))!!
    val constructor = env.GetMethodID(cls, "<init>", "(Ljava/nio/ByteBuffer;)V")!!
    return memScoped {
        val args = allocArray<jvalue>(1)
        args[0].l = jvmBuffer
        val newObjectA = env.pointed.pointedCommon?.NewObjectA ?: throw JniUnavailableError()
        newObjectA(env, cls, constructor, args) ?: throw OutOfMemoryError("Could not create ByteBuffer on JVM.")
    }
}

/**
 * Convert a native wrapper [ByteBuffer] to a jobject representing a java.nio.ByteBuffer.
 *
 * @return a jobject representing a java.nio.ByteBuffer or null if operation failed.
 */
@OptIn(ExperimentalForeignApi::class)
fun ByteBuffer.toJNioByteBuffer(env: CPointer<JNIEnvVar>): jobject {
    val newDirectByteBuffer = env.pointed.pointedCommon?.NewDirectByteBuffer ?: throw JniUnavailableError()
    val jvmBuffer = newDirectByteBuffer(env, address, capacity) ?: throw OutOfMemoryError("NewDirectByteBuffer could not wrap memory address ${this.address}.")
    return jvmBuffer
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

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.GetObjectClass(obj: jobject): jclass = memScoped {
    pointed.pointedCommon!!.GetObjectClass!!.invoke(this@GetObjectClass, obj) ?: throw JniUnavailableError()
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.ExceptionCheck(): UByte {
    return pointed.pointedCommon!!.ExceptionCheck!!.invoke(this)
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.ExceptionOccurred(): jthrowable? {
    return pointed.pointedCommon!!.ExceptionOccurred!!.invoke(this)
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.ExceptionClear() {
    return pointed.pointedCommon!!.ExceptionClear!!.invoke(this)
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
    return if (obj == null) null
    else NewGlobalRef(obj)
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.NewGlobalRef(obj: jobject): jobject {
    val newGlobalRef = pointed.pointedCommon?.NewGlobalRef ?: throw JniUnavailableError()
    return newGlobalRef.invoke(this, obj) ?: throw OutOfMemoryError("Could not create GlobalRef.")
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
@Throws(JvmCallException::class)
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
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallLongMethodA(
    obj: jobject,
    method: jmethodID,
    args: CPointer<jvalue>
): jlong {
    return pointed.pointedCommon!!.CallLongMethodA!!.invoke(
        this,
        obj,
        method,
        args
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallObjectMethodA(
    obj: jobject,
    method: jmethodID,
    args: CPointer<jvalue>?,
    noExceptionCheck: Boolean = false
): jobject? {
    return pointed.pointedCommon!!.CallObjectMethodA!!.invoke(
        this,
        obj,
        method,
        args
    ).also {
        if (!noExceptionCheck) checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallDoubleMethodA(
    obj: jobject,
    method: jmethodID,
    args: CPointer<jvalue>
): jdouble {
    return pointed.pointedCommon!!.CallDoubleMethodA!!.invoke(
        this,
        obj,
        method,
        args
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
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
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
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
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
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
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
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
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
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
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallStaticVoidMethodA(
    cls: jclass,
    method: jmethodID,
    args: CPointer<jvalue>
) {
    return pointed.pointedCommon!!.CallStaticVoidMethodA!!.invoke(
        this,
        cls,
        method,
        args
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun <T> CPointer<JNIEnvVar>.guardNullable(block: () -> T?): T? {
    val result = block()
    if (result == null) {
        // Either real null result or an exception
        checkException()
        return result
    } else return result
}

@OptIn(ExperimentalForeignApi::class)
private fun <T> CPointer<JNIEnvVar>.guardNonNull(block: () -> T?): T {
    val result = block()
    return result ?: run {
        checkException() // if this does not throw, we just throw something else.
        error("Unknown exception occurred when calling JVM from native")
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallStaticObjMethodA(
    cls: jclass,
    method: jmethodID,
    args: CPointer<jvalue>
): jobject = guardNonNull {
    pointed.pointedCommon!!.CallStaticObjectMethodA!!.invoke(
        this,
        cls,
        method,
        args
    )
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallStaticObjMethodANullable(
    cls: jclass,
    method: jmethodID,
    args: CPointer<jvalue>
): jobject? = guardNullable {
    pointed.pointedCommon!!.CallStaticObjectMethodA!!.invoke(
        this,
        cls,
        method,
        args
    )
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallStaticIntMethodA(
    cls: jclass,
    method: jmethodID,
    args: CPointer<jvalue>
): jint {
    return pointed.pointedCommon!!.CallStaticIntMethodA!!.invoke(
        this,
        cls,
        method,
        args
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallStaticLongMethodA(
    cls: jclass,
    method: jmethodID,
    args: CPointer<jvalue>
): jlong {
    return pointed.pointedCommon!!.CallStaticLongMethodA!!.invoke(
        this,
        cls,
        method,
        args
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallStaticFloatMethodA(
    cls: jclass,
    method: jmethodID,
    args: CPointer<jvalue>
): jfloat {
    return pointed.pointedCommon!!.CallStaticFloatMethodA!!.invoke(
        this,
        cls,
        method,
        args
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallStaticDoubleMethodA(
    cls: jclass,
    method: jmethodID,
    args: CPointer<jvalue>
): jdouble {
    return pointed.pointedCommon!!.CallStaticDoubleMethodA!!.invoke(
        this,
        cls,
        method,
        args
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallStaticBooleanMethodA(
    cls: jclass,
    method: jmethodID,
    args: CPointer<jvalue>
): jboolean {
    return pointed.pointedCommon!!.CallStaticBooleanMethodA!!.invoke(
        this,
        cls,
        method,
        args
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallStaticShortMethodA(
    cls: jclass,
    method: jmethodID,
    args: CPointer<jvalue>
): jshort {
    return pointed.pointedCommon!!.CallStaticShortMethodA!!.invoke(
        this,
        cls,
        method,
        args
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallStaticByteMethodA(
    cls: jclass,
    method: jmethodID,
    args: CPointer<jvalue>
): jbyte {
    return pointed.pointedCommon!!.CallStaticByteMethodA!!.invoke(
        this,
        cls,
        method,
        args
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(JvmCallException::class)
fun CPointer<JNIEnvVar>.CallStaticCharMethodA(
    cls: jclass,
    method: jmethodID,
    args: CPointer<jvalue>
): jchar {
    return pointed.pointedCommon!!.CallStaticCharMethodA!!.invoke(
        this,
        cls,
        method,
        args
    ).also {
        checkException()
    }
}

@OptIn(ExperimentalForeignApi::class)
fun Any.asLongPointer(): Long {
    val stableRef = StableRef.create(this)
    return stableRef.asCPointer().toLong()
}

@OptIn(ExperimentalForeignApi::class)
inline fun <reified T: Any> Long.fromLongPointer(): T {
    val rendererRef = toCPointer<COpaque>()?.asStableRef<T>() ?: error("Could not convert $this to pointer.")
    return rendererRef.get()
}

@OptIn(ExperimentalForeignApi::class)
inline fun <reified T: AutoCloseable> Long.releaseStableRef() {
    val pointer = toCPointer<CPointed>()
    pointer?.asStableRef<T>()?.dispose()
}