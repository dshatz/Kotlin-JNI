package dev.datlag.nkommons

import dev.datlag.nkommons.binding.jarray
import dev.datlag.nkommons.binding.jboolean
import dev.datlag.nkommons.binding.jbooleanArray
import dev.datlag.nkommons.binding.jbooleanVar
import dev.datlag.nkommons.binding.jbyte
import dev.datlag.nkommons.binding.jbyteArray
import dev.datlag.nkommons.binding.jbyteVar
import dev.datlag.nkommons.binding.jchar
import dev.datlag.nkommons.binding.jcharArray
import dev.datlag.nkommons.binding.jcharVar
import dev.datlag.nkommons.binding.jclass
import dev.datlag.nkommons.binding.jdouble
import dev.datlag.nkommons.binding.jdoubleArray
import dev.datlag.nkommons.binding.jdoubleVar
import dev.datlag.nkommons.binding.jfieldID
import dev.datlag.nkommons.binding.jfloat
import dev.datlag.nkommons.binding.jfloatArray
import dev.datlag.nkommons.binding.jfloatVar
import dev.datlag.nkommons.binding.jint
import dev.datlag.nkommons.binding.jintArray
import dev.datlag.nkommons.binding.jintVar
import dev.datlag.nkommons.binding.jlong
import dev.datlag.nkommons.binding.jlongArray
import dev.datlag.nkommons.binding.jlongVar
import dev.datlag.nkommons.binding.jmethodID
import dev.datlag.nkommons.binding.jobject
import dev.datlag.nkommons.binding.jobjectArray
import dev.datlag.nkommons.binding.jobjectRefType
import dev.datlag.nkommons.binding.jshort
import dev.datlag.nkommons.binding.jshortArray
import dev.datlag.nkommons.binding.jshortVar
import dev.datlag.nkommons.binding.jsize
import dev.datlag.nkommons.binding.jstring
import dev.datlag.nkommons.binding.jthrowable
import dev.datlag.nkommons.binding.jweak
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret

@OptIn(ExperimentalForeignApi::class)
actual class CommonJNINativeInterface(
    actual val platform: JNINativeInterface
) {

    actual var NewLongArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jlongArray?>>?
        get() = platform.NewLongArray
        set(value) {
            platform.NewLongArray = value
        }

    actual var GetByteArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbyteArray?, CPointer<jbooleanVar>?) -> CPointer<ByteVarOf<jbyte>>?>>?
        get() = platform.GetByteArrayElements
        set(value) {
            platform.GetByteArrayElements = value
        }

    actual var ExceptionClear: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> Unit>>?
        get() = platform.ExceptionClear
        set(value) {
            platform.ExceptionClear = value
        }

    actual var CallNonvirtualCharMethod: COpaquePointer?
        get() = platform.CallNonvirtualCharMethod
        set(value) {
            platform.CallNonvirtualCharMethod = value
        }

    actual var SetStaticByteField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jbyte) -> Unit>>?
        get() = platform.SetStaticByteField?.reinterpret()
        set(value) {
            platform.SetStaticByteField = value?.reinterpret()
        }

    actual var CallNonvirtualDoubleMethod: COpaquePointer?
        get() = platform.CallNonvirtualDoubleMethod
        set(value) {
            platform.CallNonvirtualDoubleMethod = value
        }

    actual var CallStaticFloatMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jfloat>>?
        get() = platform.CallStaticFloatMethodA?.reinterpret()
        set(value) {
            platform.CallStaticFloatMethodA = value?.reinterpret()
        }

    actual var CallStaticLongMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jlong>>?
        get() = platform.CallStaticLongMethodA?.reinterpret()
        set(value) {
            platform.CallStaticLongMethodA = value?.reinterpret()
        }

    actual var CallBooleanMethod: COpaquePointer?
        get() = platform.CallBooleanMethod
        set(value) {
            platform.CallBooleanMethod = value
        }

    actual var GetStringUTFLength: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?) -> jsize>>?
        get() = platform.GetStringUTFLength
        set(value) {
            platform.GetStringUTFLength = value
        }

    actual var CallVoidMethod: COpaquePointer?
        get() = platform.CallVoidMethod
        set(value) {
            platform.CallVoidMethod = value
        }

    actual var Throw: CPointer<CFunction<(CPointer<JNIEnvVar>?, jthrowable?) -> jint>>?
        get() = platform.Throw
        set(value) {
            platform.Throw = value
        }

    actual var NewByteArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jbyteArray?>>?
        get() = platform.NewByteArray
        set(value) {
            platform.NewByteArray = value
        }

    actual var CallLongMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jlong>>?
        get() = platform.CallLongMethodA?.reinterpret()
        set(value) {
            platform.CallLongMethodA = value?.reinterpret()
        }

    actual var FatalError: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<ByteVarOf<Byte>>?) -> Unit>>?
        get() = platform.FatalError
        set(value) {
            platform.FatalError = value
        }

    actual var NewCharArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jcharArray?>>?
        get() = platform.NewCharArray
        set(value) {
            platform.NewCharArray = value
        }

    actual var CallFloatMethod: COpaquePointer?
        get() = platform.CallFloatMethod
        set(value) {
            platform.CallFloatMethod = value
        }

    actual var EnsureLocalCapacity: CPointer<CFunction<(CPointer<JNIEnvVar>?, jint) -> jint>>?
        get() = platform.EnsureLocalCapacity
        set(value) {
            platform.EnsureLocalCapacity = value
        }

    actual var CallNonvirtualObjectMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jobject?>>?
        get() = platform.CallNonvirtualObjectMethodA?.reinterpret()
        set(value) {
            platform.CallNonvirtualObjectMethodA = value?.reinterpret()
        }

    actual var AllocObject: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?) -> jobject?>>?
        get() = platform.AllocObject
        set(value) {
            platform.AllocObject = value
        }

    actual var ExceptionOccurred: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> jthrowable?>>?
        get() = platform.ExceptionOccurred
        set(value) {
            platform.ExceptionOccurred = value
        }

    actual var GetObjectField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jobject?>>?
        get() = platform.GetObjectField?.reinterpret()
        set(value) {
            platform.GetObjectField = value?.reinterpret()
        }

    actual var GetStaticBooleanField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jboolean>>?
        get() = platform.GetStaticBooleanField?.reinterpret()
        set(value) {
            platform.GetStaticBooleanField = value?.reinterpret()
        }

    actual var SetIntField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jint) -> Unit>>?
        get() = platform.SetIntField?.reinterpret()
        set(value) {
            platform.SetIntField = value?.reinterpret()
        }

    actual var ReleaseIntArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jintArray?, CPointer<jintVar>?, jint) -> Unit>>?
        get() = platform.ReleaseIntArrayElements
        set(value) {
            platform.ReleaseIntArrayElements = value
        }

    actual var CallStaticFloatMethod: COpaquePointer?
        get() = platform.CallStaticFloatMethod
        set(value) {
            platform.CallStaticFloatMethod = value
        }

    actual var GetStaticDoubleField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jdouble>>?
        get() = platform.GetStaticDoubleField?.reinterpret()
        set(value) {
            platform.GetStaticDoubleField = value?.reinterpret()
        }

    actual var GetStaticIntField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jint>>?
        get() = platform.GetStaticIntField?.reinterpret()
        set(value) {
            platform.GetStaticIntField = value?.reinterpret()
        }

    actual var GetJavaVM: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<CPointerVarOf<CPointer<JavaVMVar>>>?) -> jint>>?
        get() = platform.GetJavaVM
        set(value) {
            platform.GetJavaVM = value
        }

    actual var GetFieldID: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, CPointer<ByteVar>?, CPointer<ByteVar>?) -> jfieldID?>>?
        get() = platform.GetFieldID?.reinterpret()
        set(value) {
            platform.GetFieldID = value?.reinterpret()
        }

    actual var GetLongArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jlongArray?, jsize, jsize, CPointer<jlongVar>?) -> Unit>>?
        get() = platform.GetLongArrayRegion
        set(value) {
            platform.GetLongArrayRegion = value
        }

    actual var SetStaticLongField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jlong) -> Unit>>?
        get() = platform.SetStaticLongField?.reinterpret()
        set(value) {
            platform.SetStaticLongField = value?.reinterpret()
        }

    actual var ExceptionDescribe: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> Unit>>?
        get() = platform.ExceptionDescribe
        set(value) {
            platform.ExceptionDescribe = value
        }

    actual var CallShortMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jshort>>?
        get() = platform.CallShortMethodA?.reinterpret()
        set(value) {
            platform.CallShortMethodA = value?.reinterpret()
        }

    actual var CallStaticVoidMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> Unit>>?
        get() = platform.CallStaticVoidMethodA?.reinterpret()
        set(value) {
            platform.CallStaticVoidMethodA = value?.reinterpret()
        }

    actual var ReleaseStringUTFChars: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, CPointer<ByteVar>?) -> Unit>>?
        get() = platform.ReleaseStringUTFChars
        set(value) {
            platform.ReleaseStringUTFChars = value
        }

    actual var CallStaticCharMethod: COpaquePointer?
        get() = platform.CallStaticCharMethod
        set(value) {
            platform.CallStaticCharMethod = value
        }

    actual var NewGlobalRef: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jobject?>>?
        get() = platform.NewGlobalRef
        set(value) {
            platform.NewGlobalRef = value
        }

    actual var SetCharArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jcharArray?, jsize, jsize, CPointer<jcharVar>?) -> Unit>>?
        get() = platform.SetCharArrayRegion
        set(value) {
            platform.SetCharArrayRegion = value
        }

    actual var CallNonvirtualVoidMethod: COpaquePointer?
        get() = platform.CallNonvirtualVoidMethod
        set(value) {
            platform.CallNonvirtualVoidMethod = value
        }

    actual var ExceptionCheck: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> jboolean>>?
        get() = platform.ExceptionCheck
        set(value) {
            platform.ExceptionCheck = value
        }

    actual var CallCharMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jchar>>?
        get() = platform.CallCharMethodA?.reinterpret()
        set(value) {
            platform.CallCharMethodA = value?.reinterpret()
        }

    actual var GetByteField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jbyte>>?
        get() = platform.GetByteField?.reinterpret()
        set(value) {
            platform.GetByteField = value?.reinterpret()
        }

    actual var FindClass: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<ByteVar>?) -> jclass?>>?
        get() = platform.FindClass
        set(value) {
            platform.FindClass = value
        }

    actual var SetByteField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jbyte) -> Unit>>?
        get() = platform.SetByteField?.reinterpret()
        set(value) {
            platform.SetByteField = value?.reinterpret()
        }

    actual var SetStaticShortField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jshort) -> Unit>>?
        get() = platform.SetStaticShortField?.reinterpret()
        set(value) {
            platform.SetStaticShortField = value?.reinterpret()
        }

    actual var CallLongMethod: COpaquePointer?
        get() = platform.CallLongMethod
        set(value) {
            platform.CallLongMethod = value
        }

    actual var PopLocalFrame: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jobject?>>?
        get() = platform.PopLocalFrame
        set(value) {
            platform.PopLocalFrame = value
        }

    actual var SetStaticDoubleField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jdouble) -> Unit>>?
        get() = platform.SetStaticDoubleField?.reinterpret()
        set(value) {
            platform.SetStaticDoubleField = value?.reinterpret()
        }

    actual var CallIntMethod: COpaquePointer?
        get() = platform.CallIntMethod
        set(value) {
            platform.CallIntMethod = value
        }

    actual var GetBooleanField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jboolean>>?
        get() = platform.GetBooleanField?.reinterpret()
        set(value) {
            platform.GetBooleanField = value?.reinterpret()
        }

    actual var GetStaticMethodID: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, CPointer<ByteVar>?, CPointer<ByteVar>?) -> jmethodID?>>?
        get() = platform.GetStaticMethodID?.reinterpret()
        set(value) {
            platform.GetStaticMethodID = value?.reinterpret()
        }

    actual var GetObjectArrayElement: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobjectArray?, jsize) -> jobject?>>?
        get() = platform.GetObjectArrayElement
        set(value) {
            platform.GetObjectArrayElement = value
        }

    actual var CallStaticDoubleMethod: COpaquePointer?
        get() = platform.CallStaticDoubleMethod
        set(value) {
            platform.CallStaticDoubleMethod = value
        }

    actual var CallVoidMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> Unit>>?
        get() = platform.CallVoidMethodA?.reinterpret()
        set(value) {
            platform.CallVoidMethodA = value?.reinterpret()
        }

    actual var reserved1: COpaquePointer?
        get() = platform.reserved1
        set(value) {
            platform.reserved1 = value
        }

    actual var NewShortArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jshortArray?>>?
        get() = platform.NewShortArray
        set(value) {
            platform.NewShortArray = value
        }

    actual var PushLocalFrame: CPointer<CFunction<(CPointer<JNIEnvVar>?, jint) -> jint>>?
        get() = platform.PushLocalFrame
        set(value) {
            platform.PushLocalFrame
        }

    actual var ReleaseByteArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbyteArray?, CPointer<jbyteVar>?, jint) -> Unit>>?
        get() = platform.ReleaseByteArrayElements
        set(value) {
            platform.ReleaseByteArrayElements = value
        }

    actual var CallNonvirtualFloatMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jfloat>>?
        get() = platform.CallNonvirtualFloatMethodA?.reinterpret()
        set(value) {
            platform.CallNonvirtualFloatMethodA = value?.reinterpret()
        }

    actual var CallNonvirtualByteMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jbyte>>?
        get() = platform.CallNonvirtualByteMethodA?.reinterpret()
        set(value) {
            platform.CallNonvirtualByteMethodA = value?.reinterpret()
        }

    actual var GetPrimitiveArrayCritical: CPointer<CFunction<(CPointer<JNIEnvVar>?, jarray?, CPointer<jbooleanVar>?) -> COpaquePointer?>>?
        get() = platform.GetPrimitiveArrayCritical
        set(value) {
            platform.GetPrimitiveArrayCritical = value
        }

    actual var DeleteWeakGlobalRef: CPointer<CFunction<(CPointer<JNIEnvVar>?, jweak?) -> Unit>>?
        get() = platform.DeleteWeakGlobalRef
        set(value) {
            platform.DeleteWeakGlobalRef = value
        }

    actual var SetCharField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jchar) -> Unit>>?
        get() = platform.SetCharField?.reinterpret()
        set(value) {
            platform.SetCharField = value?.reinterpret()
        }

    actual var GetCharArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jcharArray?, CPointer<jbooleanVar>?) -> CPointer<jcharVar>?>>?
        get() = platform.GetCharArrayElements
        set(value) {
            platform.GetCharArrayElements = value
        }

    actual var GetStringLength: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?) -> jsize>>?
        get() = platform.GetStringLength
        set(value) {
            platform.GetStringLength = value
        }

    actual var CallNonvirtualShortMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jshort>>?
        get() = platform.CallNonvirtualShortMethodA?.reinterpret()
        set(value) {
            platform.CallNonvirtualShortMethodA = value?.reinterpret()
        }

    actual var CallStaticShortMethod: COpaquePointer?
        get() = platform.CallStaticShortMethod
        set(value) {
            platform.CallStaticShortMethod = value
        }

    actual var GetBooleanArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbooleanArray?, CPointer<jbooleanVar>?) -> CPointer<jbooleanVar>?>>?
        get() = platform.GetBooleanArrayElements
        set(value) {
            platform.GetBooleanArrayElements = value
        }

    actual var SetObjectField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jobject?) -> Unit>>?
        get() = platform.SetObjectField?.reinterpret()
        set(value) {
            platform.SetObjectField = value?.reinterpret()
        }

    actual var GetSuperclass: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?) -> jclass?>>?
        get() = platform.GetSuperclass
        set(value) {
            platform.GetSuperclass = value
        }

    actual var GetStringUTFRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, jsize, jsize, CPointer<ByteVar>?) -> Unit>>?
        get() = platform.GetStringUTFRegion
        set(value) {
            platform.GetStringUTFRegion = value
        }

    actual var SetLongArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jlongArray?, jsize, jsize, CPointer<jlongVar>?) -> Unit>>?
        get() = platform.SetLongArrayRegion
        set(value) {
            platform.SetLongArrayRegion = value
        }

    actual var RegisterNatives: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, CPointer<JNINativeMethod>?, jint) -> jint>>?
        get() = platform.RegisterNatives
        set(value) {
            platform.RegisterNatives = value
        }

    actual var SetIntArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jintArray?, jsize, jsize, CPointer<jintVar>?) -> Unit>>?
        get() = platform.SetIntArrayRegion
        set(value) {
            platform.SetIntArrayRegion = value
        }

    actual var SetFloatField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jfloat) -> Unit>>?
        get() = platform.SetFloatField?.reinterpret()
        set(value) {
            platform.SetFloatField = value?.reinterpret()
        }

    actual var NewBooleanArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jbooleanArray?>>?
        get() = platform.NewBooleanArray
        set(value) {
            platform.NewBooleanArray = value
        }

    actual var GetDoubleArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jdoubleArray?, CPointer<jbooleanVar>?) -> CPointer<jdoubleVar>?>>?
        get() = platform.GetDoubleArrayElements
        set(value) {
            platform.GetDoubleArrayElements = value
        }

    actual var CallStaticIntMethod: COpaquePointer?
        get() = platform.CallStaticIntMethod
        set(value) {
            platform.CallStaticIntMethod = value
        }

    actual var CallStaticByteMethod: COpaquePointer?
        get() = platform.CallStaticByteMethod
        set(value) {
            platform.CallStaticByteMethod = value
        }

    actual var CallFloatMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jfloat>>?
        get() = platform.CallFloatMethodA?.reinterpret()
        set(value) {
            platform.CallFloatMethodA = value?.reinterpret()
        }

    actual var SetByteArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbyteArray?, jsize, jsize, CPointer<jbyteVar>?) -> Unit>>?
        get() = platform.SetByteArrayRegion
        set(value) {
            platform.SetByteArrayRegion = value
        }

    actual var GetStaticShortField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jshort>>?
        get() = platform.GetStaticShortField?.reinterpret()
        set(value) {
            platform.GetStaticShortField = value?.reinterpret()
        }

    actual var CallStaticShortMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jshort>>?
        get() = platform.CallStaticShortMethodA?.reinterpret()
        set(value) {
            platform.CallStaticShortMethodA = value?.reinterpret()
        }

    actual var ReleaseBooleanArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbooleanArray?, CPointer<jbooleanVar>?, jint) -> Unit>>?
        get() = platform.ReleaseBooleanArrayElements
        set(value) {
            platform.ReleaseBooleanArrayElements = value
        }

    actual var GetStaticObjectField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jobject?>>?
        get() = platform.GetStaticObjectField?.reinterpret()
        set(value) {
            platform.GetStaticObjectField = value?.reinterpret()
        }

    actual var MonitorEnter: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jint>>?
        get() = platform.MonitorEnter
        set(value) {
            platform.MonitorExit = value
        }

    actual var CallNonvirtualLongMethod: COpaquePointer?
        get() = platform.CallNonvirtualLongMethod
        set(value) {
            platform.CallNonvirtualLongMethod = value
        }

    actual var SetLongField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jlong) -> Unit>>?
        get() = platform.SetLongField?.reinterpret()
        set(value) {
            platform.SetLongField = value?.reinterpret()
        }

    actual var ReleaseStringCritical: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, CPointer<jcharVar>?) -> Unit>>?
        get() = platform.ReleaseStringCritical
        set(value) {
            platform.ReleaseStringCritical = value
        }

    actual var GetFloatField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jfloat>>?
        get() = platform.GetFloatField?.reinterpret()
        set(value) {
            platform.GetFloatField = value?.reinterpret()
        }

    actual var CallByteMethod: COpaquePointer?
        get() = platform.CallByteMethod
        set(value) {
            platform.CallByteMethod = value
        }

    actual var ReleasePrimitiveArrayCritical: CPointer<CFunction<(CPointer<JNIEnvVar>?, jarray?, COpaquePointer?, jint) -> Unit>>?
        get() = platform.ReleasePrimitiveArrayCritical
        set(value) {
            platform.ReleasePrimitiveArrayCritical = value
        }

    actual var GetIntField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jint>>?
        get() = platform.GetIntField?.reinterpret()
        set(value) {
            platform.GetIntField = value?.reinterpret()
        }

    actual var GetObjectRefType: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jobjectRefType>>?
        get() = platform.GetObjectRefType
        set(value) {
            platform.GetObjectRefType = value
        }

    actual var GetFloatArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jfloatArray?, jsize, jsize, CPointer<jfloatVar>?) -> Unit>>?
        get() = platform.GetFloatArrayRegion
        set(value) {
            platform.GetFloatArrayRegion = value
        }

    actual var GetDoubleArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jdoubleArray?, jsize, jsize, CPointer<jdoubleVar>?) -> Unit>>?
        get() = platform.GetDoubleArrayRegion
        set(value) {
            platform.GetDoubleArrayRegion = value
        }

    actual var NewObject: COpaquePointer?
        get() = platform.NewObject
        set(value) {
            platform.NewObject = value
        }

    actual var CallObjectMethod: COpaquePointer?
        get() = platform.CallObjectMethod
        set(value) {
            platform.CallObjectMethod = value
        }

    actual var SetDoubleField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jdouble) -> Unit>>?
        get() = platform.SetDoubleField?.reinterpret()
        set(value) {
            platform.SetDoubleField = value?.reinterpret()
        }

    actual var CallBooleanMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jboolean>>?
        get() = platform.CallBooleanMethodA?.reinterpret()
        set(value) {
            platform.CallBooleanMethodA = value?.reinterpret()
        }

    actual var GetShortArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jshortArray?, CPointer<jbooleanVar>?) -> CPointer<jshortVar>?>>?
        get() = platform.GetShortArrayElements
        set(value) {
            platform.GetShortArrayElements = value
        }

    actual var CallNonvirtualDoubleMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jdouble>>?
        get() = platform.CallNonvirtualDoubleMethodA?.reinterpret()
        set(value) {
            platform.CallNonvirtualDoubleMethodA = value?.reinterpret()
        }

    actual var FromReflectedMethod: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jmethodID?>>?
        get() = platform.FromReflectedMethod?.reinterpret()
        set(value) {
            platform.FromReflectedMethod = value?.reinterpret()
        }

    actual var CallNonvirtualIntMethod: COpaquePointer?
        get() = platform.CallNonvirtualIntMethod
        set(value) {
            platform.CallNonvirtualIntMethod = value
        }

    actual var DeleteLocalRef: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> Unit>>?
        get() = platform.DeleteLocalRef
        set(value) {
            platform.DeleteLocalRef = value
        }

    actual var CallNonvirtualVoidMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> Unit>>?
        get() = platform.CallNonvirtualVoidMethodA?.reinterpret()
        set(value) {
            platform.CallNonvirtualVoidMethodA = value?.reinterpret()
        }

    actual var ReleaseShortArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jshortArray?, CPointer<jshortVar>?, jint) -> Unit>>?
        get() = platform.ReleaseShortArrayElements
        set(value) {
            platform.ReleaseShortArrayElements = value
        }

    actual var ToReflectedMethod: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, jboolean) -> jobject?>>?
        get() = platform.ToReflectedMethod?.reinterpret()
        set(value) {
            platform.ToReflectedMethod = value?.reinterpret()
        }

    actual var GetDirectBufferCapacity: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jlong>>?
        get() = platform.GetDirectBufferCapacity
        set(value) {
            platform.GetDirectBufferCapacity = value
        }

    actual var CallNonvirtualByteMethod: COpaquePointer?
        get() = platform.CallNonvirtualByteMethod
        set(value) {
            platform.CallNonvirtualByteMethod = value
        }

    actual var CallNonvirtualCharMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jchar>>?
        get() = platform.CallNonvirtualCharMethodA?.reinterpret()
        set(value) {
            platform.CallNonvirtualCharMethodA = value?.reinterpret()
        }

    actual var GetStaticCharField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jchar>>?
        get() = platform.GetStaticCharField?.reinterpret()
        set(value) {
            platform.GetStaticCharField = value?.reinterpret()
        }

    actual var NewDirectByteBuffer: CPointer<CFunction<(CPointer<JNIEnvVar>?, COpaquePointer?, jlong) -> jobject?>>?
        get() = platform.NewDirectByteBuffer
        set(value) {
            platform.NewDirectByteBuffer = value
        }

    actual var UnregisterNatives: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?) -> jint>>?
        get() = platform.UnregisterNatives
        set(value) {
            platform.UnregisterNatives = value
        }

    actual var GetDirectBufferAddress: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> COpaquePointer?>>?
        get() = platform.GetDirectBufferAddress
        set(value) {
            platform.GetDirectBufferAddress = value
        }

    actual var GetShortField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jshort>>?
        get() = platform.GetShortField?.reinterpret()
        set(value) {
            platform.GetShortField = value?.reinterpret()
        }

    actual var NewFloatArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jfloatArray?>>?
        get() = platform.NewFloatArray
        set(value) {
            platform.NewFloatArray = value
        }

    actual var FromReflectedField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jfieldID?>>?
        get() = platform.FromReflectedField?.reinterpret()
        set(value) {
            platform.FromReflectedField = value?.reinterpret()
        }

    actual var GetStaticByteField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jbyte>>?
        get() = platform.GetStaticByteField?.reinterpret()
        set(value) {
            platform.GetStaticByteField = value?.reinterpret()
        }

    actual var GetStaticLongField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jlong>>?
        get() = platform.GetStaticLongField?.reinterpret()
        set(value) {
            platform.GetStaticLongField = value?.reinterpret()
        }

    actual var SetFloatArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jfloatArray?, jsize, jsize, CPointer<jfloatVar>?) -> Unit>>?
        get() = platform.SetFloatArrayRegion
        set(value) {
            platform.SetFloatArrayRegion = value
        }

    actual var CallNonvirtualIntMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jint>>?
        get() = platform.CallNonvirtualIntMethodA?.reinterpret()
        set(value) {
            platform.CallNonvirtualIntMethodA = value?.reinterpret()
        }

    actual var GetStringUTFChars: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, CPointer<jbooleanVar>?) -> CPointer<ByteVar>?>>?
        get() = platform.GetStringUTFChars
        set(value) {
            platform.GetStringUTFChars = value
        }

    actual var ReleaseFloatArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jfloatArray?, CPointer<jfloatVar>?, jint) -> Unit>>?
        get() = platform.ReleaseFloatArrayElements
        set(value) {
            platform.ReleaseFloatArrayElements = value
        }

    actual var IsAssignableFrom: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jclass?) -> jboolean>>?
        get() = platform.IsAssignableFrom
        set(value) {
            platform.IsAssignableFrom = value
        }

    actual var SetShortField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jshort) -> Unit>>?
        get() = platform.SetShortField?.reinterpret()
        set(value) {
            platform.SetShortField = value?.reinterpret()
        }

    actual var CallByteMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jbyte>>?
        get() = platform.CallByteMethodA?.reinterpret()
        set(value) {
            platform.CallByteMethodA = value?.reinterpret()
        }

    actual var ReleaseCharArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jcharArray?, CPointer<jcharVar>?, jint) -> Unit>>?
        get() = platform.ReleaseCharArrayElements
        set(value) {
            platform.ReleaseCharArrayElements = value
        }

    actual var NewObjectArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize, jclass?, jobject?) -> jobjectArray?>>?
        get() = platform.NewObjectArray
        set(value) {
            platform.NewObjectArray = value
        }

    actual var CallStaticIntMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jint>>?
        get() = platform.CallStaticIntMethodA?.reinterpret()
        set(value) {
            platform.CallStaticIntMethodA = value?.reinterpret()
        }

    actual var GetObjectClass: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jclass?>>?
        get() = platform.GetObjectClass
        set(value) {
            platform.GetObjectClass = value
        }

    actual var GetStringCritical: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, CPointer<jbooleanVar>?) -> CPointer<jcharVar>?>>?
        get() = platform.GetStringCritical
        set(value) {
            platform.GetStringCritical = value
        }

    actual var CallStaticByteMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jbyte>>?
        get() = platform.CallStaticByteMethodA?.reinterpret()
        set(value) {
            platform.CallStaticByteMethodA = value?.reinterpret()
        }

    actual var ReleaseDoubleArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jdoubleArray?, CPointer<jdoubleVar>?, jint) -> Unit>>?
        get() = platform.ReleaseDoubleArrayElements
        set(value) {
            platform.ReleaseDoubleArrayElements = value
        }

    actual var GetStaticFieldID: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, CPointer<ByteVar>?, CPointer<ByteVar>?) -> jfieldID?>>?
        get() = platform.GetStaticFieldID?.reinterpret()
        set(value) {
            platform.GetStaticFieldID = value?.reinterpret()
        }

    actual var SetBooleanArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbooleanArray?, jsize, jsize, CPointer<jbooleanVar>?) -> Unit>>?
        get() = platform.SetBooleanArrayRegion
        set(value) {
            platform.SetBooleanArrayRegion = value
        }

    actual var reserved2: COpaquePointer?
        get() = platform.reserved2
        set(value) {
            platform.reserved2 = value
        }

    actual var NewStringUTF: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<ByteVar>?) -> jstring?>>?
        get() = platform.NewStringUTF
        set(value) {
            platform.NewStringUTF = value
        }

    actual var SetObjectArrayElement: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobjectArray?, jsize, jobject?) -> Unit>>?
        get() = platform.SetObjectArrayElement
        set(value) {
            platform.SetObjectArrayElement = value
        }

    actual var CallNonvirtualShortMethod: COpaquePointer?
        get() = platform.CallNonvirtualShortMethod
        set(value) {
            platform.CallNonvirtualShortMethod = value
        }

    actual var GetCharArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jcharArray?, jsize, jsize, CPointer<jcharVar>?) -> Unit>>?
        get() = platform.GetCharArrayRegion
        set(value) {
            platform.GetCharArrayRegion = value
        }

    actual var GetFloatArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jfloatArray?, CPointer<jbooleanVar>?) -> CPointer<jfloatVar>?>>?
        get() = platform.GetFloatArrayElements
        set(value) {
            platform.GetFloatArrayElements = value
        }

    actual var CallObjectMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jobject?>>?
        get() = platform.CallObjectMethodA?.reinterpret()
        set(value) {
            platform.CallObjectMethodA = value?.reinterpret()
        }

    actual var NewWeakGlobalRef: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jweak?>>?
        get() = platform.NewWeakGlobalRef
        set(value) {
            platform.NewWeakGlobalRef = value
        }

    actual var CallCharMethod: COpaquePointer?
        get() = platform.CallCharMethod
        set(value) {
            platform.CallCharMethod = value
        }

    actual var CallNonvirtualFloatMethod: COpaquePointer?
        get() = platform.CallNonvirtualFloatMethod
        set(value) {
            platform.CallNonvirtualFloatMethod = value
        }

    actual var NewDoubleArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jdoubleArray?>>?
        get() = platform.NewDoubleArray
        set(value) {
            platform.NewDoubleArray = value
        }

    actual var CallNonvirtualBooleanMethod: COpaquePointer?
        get() = platform.CallNonvirtualBooleanMethod
        set(value) {
            platform.CallNonvirtualBooleanMethod = value
        }

    actual var SetStaticIntField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jint) -> Unit>>?
        get() = platform.SetStaticIntField?.reinterpret()
        set(value) {
            platform.SetStaticIntField = value?.reinterpret()
        }

    actual var ToReflectedField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jboolean) -> jobject?>>?
        get() = platform.ToReflectedField?.reinterpret()
        set(value) {
            platform.ToReflectedField = value?.reinterpret()
        }

    actual var GetStringRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, jsize, jsize, CPointer<jcharVar>?) -> Unit>>?
        get() = platform.GetStringRegion
        set(value) {
            platform.GetStringRegion = value
        }

    actual var GetShortArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jshortArray?, jsize, jsize, CPointer<jshortVar>?) -> Unit>>?
        get() = platform.GetShortArrayRegion
        set(value) {
            platform.GetShortArrayRegion = value
        }

    actual var MonitorExit: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jint>>?
        get() = platform.MonitorExit
        set(value) {
            platform.MonitorExit = value
        }

    actual var CallStaticLongMethod: COpaquePointer?
        get() = platform.CallStaticLongMethod
        set(value) {
            platform.CallStaticLongMethod = value
        }

    actual var CallShortMethod: COpaquePointer?
        get() = platform.CallShortMethod
        set(value) {
            platform.CallShortMethod = value
        }

    actual var CallNonvirtualLongMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jlong>>?
        get() = platform.CallNonvirtualLongMethodA?.reinterpret()
        set(value) {
            platform.CallNonvirtualLongMethodA = value?.reinterpret()
        }

    actual var GetMethodID: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, CPointer<ByteVar>?, CPointer<ByteVar>?) -> jmethodID?>>?
        get() = platform.GetMethodID?.reinterpret()
        set(value) {
            platform.GetMethodID = value?.reinterpret()
        }

    actual var CallIntMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jint>>?
        get() = platform.CallIntMethodA?.reinterpret()
        set(value) {
            platform.CallIntMethodA = value?.reinterpret()
        }

    actual var DefineClass: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<ByteVar>?, jobject?, CPointer<jbyteVar>?, jsize) -> jclass?>>?
        get() = platform.DefineClass
        set(value) {
            platform.DefineClass = value
        }

    actual var GetStringChars: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, CPointer<jbooleanVar>?) -> CPointer<jcharVar>?>>?
        get() = platform.GetStringChars
        set(value) {
            platform.GetStringChars = value
        }

    actual var GetIntArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jintArray?, jsize, jsize, CPointer<jintVar>?) -> Unit>>?
        get() = platform.GetIntArrayRegion
        set(value) {
            platform.GetIntArrayRegion = value
        }

    actual var CallDoubleMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jdouble>>?
        get() = platform.CallDoubleMethodA?.reinterpret()
        set(value) {
            platform.CallDoubleMethodA = value?.reinterpret()
        }

    actual var CallStaticObjectMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jobject?>>?
        get() = platform.CallStaticObjectMethodA?.reinterpret()
        set(value) {
            platform.CallStaticObjectMethodA = value?.reinterpret()
        }

    actual var ReleaseStringChars: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, CPointer<jcharVar>?) -> Unit>>?
        get() = platform.ReleaseStringChars
        set(value) {
            platform.ReleaseStringChars = value
        }

    actual var GetStaticFloatField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jfloat>>?
        get() = platform.GetStaticFloatField?.reinterpret()
        set(value) {
            platform.GetStaticFloatField = value?.reinterpret()
        }

    actual var GetIntArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jintArray?, CPointer<jbooleanVar>?) -> CPointer<jintVar>?>>?
        get() = platform.GetIntArrayElements
        set(value) {
            platform.GetIntArrayElements = value
        }

    actual var CallStaticBooleanMethod: COpaquePointer?
        get() = platform.CallStaticBooleanMethod
        set(value) {
            platform.CallStaticBooleanMethod = value
        }

    actual var GetBooleanArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbooleanArray?, jsize, jsize, CPointer<jbooleanVar>?) -> Unit>>?
        get() = platform.GetBooleanArrayRegion
        set(value) {
            platform.GetBooleanArrayRegion = value
        }

    actual var GetVersion: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> jint>>?
        get() = platform.GetVersion
        set(value) {
            platform.GetVersion = value
        }

    actual var CallStaticObjectMethod: COpaquePointer?
        get() = platform.CallStaticObjectMethod
        set(value) {
            platform.CallStaticObjectMethod = value
        }

    actual var NewObjectA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jobject?>>?
        get() = platform.NewObjectA?.reinterpret()
        set(value) {
            platform.NewObjectA = value?.reinterpret()
        }

    actual var SetStaticCharField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jchar) -> Unit>>?
        get() = platform.SetStaticCharField?.reinterpret()
        set(value) {
            platform.SetStaticCharField = value?.reinterpret()
        }

    actual var IsSameObject: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jobject?) -> jboolean>>?
        get() = platform.IsSameObject
        set(value) {
            platform.IsSameObject = value
        }

    actual var reserved3: COpaquePointer?
        get() = platform.reserved3
        set(value) {
            platform.reserved3 = value
        }

    actual var SetStaticBooleanField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jboolean) -> Unit>>?
        get() = platform.SetStaticBooleanField?.reinterpret()
        set(value) {
            platform.SetStaticBooleanField = value?.reinterpret()
        }

    actual var SetStaticObjectField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jobject?) -> Unit>>?
        get() = platform.SetStaticObjectField?.reinterpret()
        set(value) {
            platform.SetStaticObjectField = value?.reinterpret()
        }

    actual var CallStaticBooleanMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jboolean>>?
        get() = platform.CallStaticBooleanMethodA?.reinterpret()
        set(value) {
            platform.CallStaticBooleanMethodA = value?.reinterpret()
        }

    actual var SetStaticFloatField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jfloat) -> Unit>>?
        get() = platform.SetStaticFloatField?.reinterpret()
        set(value) {
            platform.SetStaticFloatField = value?.reinterpret()
        }

    actual var CallStaticVoidMethod: COpaquePointer?
        get() = platform.CallStaticVoidMethod
        set(value) {
            platform.CallStaticVoidMethod = value
        }

    actual var CallStaticCharMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jchar>>?
        get() = platform.CallStaticCharMethodA?.reinterpret()
        set(value) {
            platform.CallStaticCharMethodA = value?.reinterpret()
        }

    actual var DeleteGlobalRef: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> Unit>>?
        get() = platform.DeleteGlobalRef
        set(value) {
            platform.DeleteGlobalRef = value
        }

    actual var IsInstanceOf: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?) -> jboolean>>?
        get() = platform.IsInstanceOf
        set(value) {
            platform.IsInstanceOf = value
        }

    actual var NewLocalRef: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jobject?>>?
        get() = platform.NewLocalRef
        set(value) {
            platform.NewLocalRef = value
        }

    actual var CallStaticDoubleMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jdouble>>?
        get() = platform.CallStaticDoubleMethodA?.reinterpret()
        set(value) {
            platform.CallStaticDoubleMethodA = value?.reinterpret()
        }

    actual var GetArrayLength: CPointer<CFunction<(CPointer<JNIEnvVar>?, jarray?) -> jsize>>?
        get() = platform.GetArrayLength
        set(value) {
            platform.GetArrayLength = value
        }

    actual var GetLongArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jlongArray?, CPointer<jbooleanVar>?) -> CPointer<jlongVar>?>>?
        get() = platform.GetLongArrayElements
        set(value) {
            platform.GetLongArrayElements = value
        }

    actual var ThrowNew: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, CPointer<ByteVar>?) -> jint>>?
        get() = platform.ThrowNew
        set(value) {
            platform.ThrowNew = value
        }

    actual var CallDoubleMethod: COpaquePointer?
        get() = platform.CallDoubleMethod
        set(value) {
            platform.CallDoubleMethod = value
        }

    actual var CallNonvirtualObjectMethod: COpaquePointer?
        get() = platform.CallNonvirtualObjectMethod
        set(value) {
            platform.CallNonvirtualObjectMethod = value
        }

    actual var SetShortArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jshortArray?, jsize, jsize, CPointer<jshortVar>?) -> Unit>>?
        get() = platform.SetShortArrayRegion
        set(value) {
            platform.SetShortArrayRegion = value
        }

    actual var NewIntArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jintArray?>>?
        get() = platform.NewIntArray
        set(value) {
            platform.NewIntArray = value
        }

    actual var GetCharField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jchar>>?
        get() = platform.GetCharField?.reinterpret()
        set(value) {
            platform.GetCharField = value?.reinterpret()
        }

    actual var reserved0: COpaquePointer?
        get() = platform.reserved0
        set(value) {
            platform.reserved0 = value
        }

    actual var GetLongField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jlong>>?
        get() = platform.GetLongField?.reinterpret()
        set(value) {
            platform.GetLongField = value?.reinterpret()
        }

    actual var SetBooleanField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jboolean) -> Unit>>?
        get() = platform.SetBooleanField?.reinterpret()
        set(value) {
            platform.SetBooleanField = value?.reinterpret()
        }

    actual var SetDoubleArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jdoubleArray?, jsize, jsize, CPointer<jdoubleVar>?) -> Unit>>?
        get() = platform.SetDoubleArrayRegion
        set(value) {
            platform.SetDoubleArrayRegion = value
        }

    actual var GetDoubleField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jdouble>>?
        get() = platform.GetDoubleField?.reinterpret()
        set(value) {
            platform.GetDoubleField = value?.reinterpret()
        }

    actual var ReleaseLongArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jlongArray?, CPointer<jlongVar>?, jint) -> Unit>>?
        get() = platform.ReleaseLongArrayElements
        set(value) {
            platform.ReleaseLongArrayElements = value
        }

    actual var CallNonvirtualBooleanMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jboolean>>?
        get() = platform.CallNonvirtualBooleanMethodA?.reinterpret()
        set(value) {
            platform.CallNonvirtualBooleanMethodA = value?.reinterpret()
        }

    actual var NewString: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<jcharVar>?, jsize) -> jstring?>>?
        get() = platform.NewString
        set(value) {
            platform.NewString = value
        }

    actual companion object {
        actual operator fun invoke(platform: JNINativeInterface) = CommonJNINativeInterface(platform)
    }
}