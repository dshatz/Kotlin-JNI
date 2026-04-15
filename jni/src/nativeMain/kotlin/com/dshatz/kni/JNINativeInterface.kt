package com.dshatz.kni

import com.dshatz.kni.binding.jboolean
import com.dshatz.kni.binding.jint
import kotlinx.cinterop.*

/**
 * JNINativeInterface which can be "implemented" using typealias.
 *
 * Fields with jobject can't be added here as these types differ slightly on android / desktop.
 */
@OptIn(ExperimentalForeignApi::class)
expect class JNINativeInterface : CStructVar {
    var ExceptionClear: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> Unit>>?
    var CallNonvirtualCharMethod: COpaquePointer?
    var CallNonvirtualDoubleMethod: COpaquePointer?
    var CallBooleanMethod: COpaquePointer?
    var CallVoidMethod: COpaquePointer?
    var FatalError: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<ByteVarOf<Byte>>?) -> Unit>>?
    var CallFloatMethod: COpaquePointer?
    var EnsureLocalCapacity: CPointer<CFunction<(CPointer<JNIEnvVar>?, jint) -> jint>>?
    var CallStaticFloatMethod: COpaquePointer?
    var GetJavaVM: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<CPointerVarOf<CPointer<JavaVMVar>>>?) -> jint>>?
    var ExceptionDescribe: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> Unit>>?
    var CallStaticCharMethod: COpaquePointer?
    var CallNonvirtualVoidMethod: COpaquePointer?
    var ExceptionCheck: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> jboolean>>?
    var CallLongMethod: COpaquePointer?
    var CallIntMethod: COpaquePointer?
    var CallStaticDoubleMethod: COpaquePointer?
    var reserved1: COpaquePointer?
    var PushLocalFrame: CPointer<CFunction<(CPointer<JNIEnvVar>?, jint) -> jint>>?
    var CallStaticShortMethod: COpaquePointer?
    var CallStaticIntMethod: COpaquePointer?
    var CallStaticByteMethod: COpaquePointer?
    var CallNonvirtualLongMethod: COpaquePointer?
    var CallByteMethod: COpaquePointer?
    var NewObject: COpaquePointer?
    var CallObjectMethod: COpaquePointer?
    var CallNonvirtualIntMethod: COpaquePointer?
    var CallNonvirtualByteMethod: COpaquePointer?
    var reserved2: COpaquePointer?
    var CallNonvirtualShortMethod: COpaquePointer?
    var CallCharMethod: COpaquePointer?
    var CallNonvirtualFloatMethod: COpaquePointer?
    var CallNonvirtualBooleanMethod: COpaquePointer?
    var CallStaticLongMethod: COpaquePointer?
    var CallShortMethod: COpaquePointer?
    var CallStaticBooleanMethod: COpaquePointer?
    var GetVersion: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> jint>>?
    var CallStaticObjectMethod: COpaquePointer?
    var reserved3: COpaquePointer?
    var CallStaticVoidMethod: COpaquePointer?
    var CallDoubleMethod: COpaquePointer?
    var CallNonvirtualObjectMethod: COpaquePointer?
    var reserved0: COpaquePointer?

    @Deprecated("Deprecated in actual type")
    companion object : Type
}