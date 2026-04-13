package com.dshatz.kni

import com.dshatz.kni.binding.jarray
import com.dshatz.kni.binding.jboolean
import com.dshatz.kni.binding.jbooleanArray
import com.dshatz.kni.binding.jbooleanVar
import com.dshatz.kni.binding.jbyte
import com.dshatz.kni.binding.jbyteArray
import com.dshatz.kni.binding.jbyteVar
import com.dshatz.kni.binding.jchar
import com.dshatz.kni.binding.jcharArray
import com.dshatz.kni.binding.jcharVar
import com.dshatz.kni.binding.jclass
import com.dshatz.kni.binding.jdouble
import com.dshatz.kni.binding.jdoubleArray
import com.dshatz.kni.binding.jdoubleVar
import com.dshatz.kni.binding.jfieldID
import com.dshatz.kni.binding.jfloat
import com.dshatz.kni.binding.jfloatArray
import com.dshatz.kni.binding.jfloatVar
import com.dshatz.kni.binding.jint
import com.dshatz.kni.binding.jintArray
import com.dshatz.kni.binding.jintVar
import com.dshatz.kni.binding.jlong
import com.dshatz.kni.binding.jlongArray
import com.dshatz.kni.binding.jlongVar
import com.dshatz.kni.binding.jmethodID
import com.dshatz.kni.binding.jobject
import com.dshatz.kni.binding.jobjectArray
import com.dshatz.kni.binding.jobjectRefType
import com.dshatz.kni.binding.jshort
import com.dshatz.kni.binding.jshortArray
import com.dshatz.kni.binding.jshortVar
import com.dshatz.kni.binding.jsize
import com.dshatz.kni.binding.jstring
import com.dshatz.kni.binding.jthrowable
import com.dshatz.kni.binding.jweak
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
expect class CommonJNINativeInterface {
    val platform: JNINativeInterface

    var NewLongArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jlongArray?>>?
    var GetByteArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbyteArray?, CPointer<jbooleanVar>?) -> CPointer<ByteVarOf<jbyte>>?>>?
    var ExceptionClear: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> Unit>>?
    var CallNonvirtualCharMethod: COpaquePointer?
    var SetStaticByteField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jbyte) -> Unit>>?
    var CallNonvirtualDoubleMethod: COpaquePointer?
    var CallStaticFloatMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jfloat>>?
    var CallStaticLongMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jlong>>?
    var CallBooleanMethod: COpaquePointer?
    var GetStringUTFLength: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?) -> jsize>>?
    var CallVoidMethod: COpaquePointer?
    var Throw: CPointer<CFunction<(CPointer<JNIEnvVar>?, jthrowable?) -> jint>>?
    var NewByteArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jbyteArray?>>?
    var CallLongMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jlong>>?
    var FatalError: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<ByteVarOf<Byte>>?) -> Unit>>?
    var NewCharArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jcharArray?>>?
    var CallFloatMethod: COpaquePointer?
    var EnsureLocalCapacity: CPointer<CFunction<(CPointer<JNIEnvVar>?, jint) -> jint>>?
    var CallNonvirtualObjectMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jobject?>>?
    var AllocObject: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?) -> jobject?>>?
    var ExceptionOccurred: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> jthrowable?>>?
    var GetObjectField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jobject?>>?

    var GetStaticBooleanField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jboolean>>?
    var SetIntField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jint) -> Unit>>?
    var ReleaseIntArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jintArray?, CPointer<jintVar>?, jint) -> Unit>>?
    var CallStaticFloatMethod: COpaquePointer?
    var GetStaticDoubleField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jdouble>>?
    var GetStaticIntField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jint>>?
    var GetJavaVM: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<CPointerVarOf<CPointer<JavaVMVar>>>?) -> jint>>?
    var GetFieldID: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, CPointer<ByteVar>?, CPointer<ByteVar>?) -> jfieldID?>>?
    var GetLongArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jlongArray?, jsize, jsize, CPointer<jlongVar>?) -> Unit>>?
    var SetStaticLongField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jlong) -> Unit>>?
    var ExceptionDescribe: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> Unit>>?
    var CallShortMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jshort>>?
    var CallStaticVoidMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> Unit>>?
    var ReleaseStringUTFChars: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, CPointer<ByteVar>?) -> Unit>>?
    var CallStaticCharMethod: COpaquePointer?
    var NewGlobalRef: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jobject?>>?
    var SetCharArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jcharArray?, jsize, jsize, CPointer<jcharVar>?) -> Unit>>?
    var CallNonvirtualVoidMethod: COpaquePointer?
    var ExceptionCheck: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> jboolean>>?
    var CallCharMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jchar>>?
    var GetByteField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jbyte>>?
    var FindClass: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<ByteVar>?) -> jclass?>>?
    var SetByteField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jbyte) -> Unit>>?
    var SetStaticShortField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jshort) -> Unit>>?
    var CallLongMethod: COpaquePointer?
    var PopLocalFrame: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jobject?>>?
    var SetStaticDoubleField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jdouble) -> Unit>>?
    var CallIntMethod: COpaquePointer?
    var GetBooleanField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jboolean>>?
    var GetStaticMethodID: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, CPointer<ByteVar>?, CPointer<ByteVar>?) -> jmethodID?>>?
    var GetObjectArrayElement: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobjectArray?, jsize) -> jobject?>>?
    var CallStaticDoubleMethod: COpaquePointer?
    var CallVoidMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> Unit>>?
    var reserved1: COpaquePointer?
    var NewShortArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jshortArray?>>?
    var PushLocalFrame: CPointer<CFunction<(CPointer<JNIEnvVar>?, jint) -> jint>>?
    var ReleaseByteArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbyteArray?, CPointer<jbyteVar>?, jint) -> Unit>>?
    var CallNonvirtualFloatMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jfloat>>?
    var CallNonvirtualByteMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jbyte>>?
    var GetPrimitiveArrayCritical: CPointer<CFunction<(CPointer<JNIEnvVar>?, jarray?, CPointer<jbooleanVar>?) -> COpaquePointer?>>?
    var DeleteWeakGlobalRef: CPointer<CFunction<(CPointer<JNIEnvVar>?, jweak?) -> Unit>>?
    var SetCharField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jchar) -> Unit>>?
    var GetCharArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jcharArray?, CPointer<jbooleanVar>?) -> CPointer<jcharVar>?>>?
    var GetStringLength: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?) -> jsize>>?
    var CallNonvirtualShortMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jshort>>?
    var CallStaticShortMethod: COpaquePointer?
    var GetBooleanArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbooleanArray?, CPointer<jbooleanVar>?) -> CPointer<jbooleanVar>?>>?
    var SetObjectField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jobject?) -> Unit>>?
    var GetSuperclass: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?) -> jclass?>>?
    var GetStringUTFRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, jsize, jsize, CPointer<ByteVar>?) -> Unit>>?
    var SetLongArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jlongArray?, jsize, jsize, CPointer<jlongVar>?) -> Unit>>?
    var RegisterNatives: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, CPointer<JNINativeMethod>?, jint) -> jint>>?
    var SetIntArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jintArray?, jsize, jsize, CPointer<jintVar>?) -> Unit>>?
    var SetFloatField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jfloat) -> Unit>>?
    var NewBooleanArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jbooleanArray?>>?
    var GetDoubleArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jdoubleArray?, CPointer<jbooleanVar>?) -> CPointer<jdoubleVar>?>>?
    var CallStaticIntMethod: COpaquePointer?
    var CallStaticByteMethod: COpaquePointer?
    var CallFloatMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jfloat>>?
    var SetByteArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbyteArray?, jsize, jsize, CPointer<jbyteVar>?) -> Unit>>?
    var GetStaticShortField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jshort>>?
    var CallStaticShortMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jshort>>?
    var ReleaseBooleanArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbooleanArray?, CPointer<jbooleanVar>?, jint) -> Unit>>?
    var GetStaticObjectField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jobject?>>?
    var MonitorEnter: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jint>>?
    var CallNonvirtualLongMethod: COpaquePointer?
    var SetLongField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jlong) -> Unit>>?
    var ReleaseStringCritical: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, CPointer<jcharVar>?) -> Unit>>?
    var GetFloatField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jfloat>>?
    var CallByteMethod: COpaquePointer?
    var ReleasePrimitiveArrayCritical: CPointer<CFunction<(CPointer<JNIEnvVar>?, jarray?, COpaquePointer?, jint) -> Unit>>?
    var GetIntField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jint>>?
    var GetObjectRefType: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jobjectRefType>>?
    var GetFloatArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jfloatArray?, jsize, jsize, CPointer<jfloatVar>?) -> Unit>>?
    var GetDoubleArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jdoubleArray?, jsize, jsize, CPointer<jdoubleVar>?) -> Unit>>?
    var NewObject: COpaquePointer?
    var CallObjectMethod: COpaquePointer?
    var SetDoubleField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jdouble) -> Unit>>?
    var CallBooleanMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jboolean>>?
    var GetShortArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jshortArray?, CPointer<jbooleanVar>?) -> CPointer<jshortVar>?>>?
    var CallNonvirtualDoubleMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jdouble>>?
    var FromReflectedMethod: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jmethodID?>>?
    var CallNonvirtualIntMethod: COpaquePointer?
    var DeleteLocalRef: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> Unit>>?
    var CallNonvirtualVoidMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> Unit>>?
    var ReleaseShortArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jshortArray?, CPointer<jshortVar>?, jint) -> Unit>>?
    var ToReflectedMethod: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, jboolean) -> jobject?>>?
    var GetDirectBufferCapacity: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jlong>>?
    var CallNonvirtualByteMethod: COpaquePointer?
    var CallNonvirtualCharMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jchar>>?
    var GetStaticCharField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jchar>>?
    var NewDirectByteBuffer: CPointer<CFunction<(CPointer<JNIEnvVar>?, COpaquePointer?, jlong) -> jobject?>>?
    var UnregisterNatives: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?) -> jint>>?
    var GetDirectBufferAddress: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> COpaquePointer?>>?
    var GetShortField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jshort>>?
    var NewFloatArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jfloatArray?>>?
    var FromReflectedField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jfieldID?>>?
    var GetStaticByteField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jbyte>>?
    var GetStaticLongField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jlong>>?
    var SetFloatArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jfloatArray?, jsize, jsize, CPointer<jfloatVar>?) -> Unit>>?
    var CallNonvirtualIntMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jint>>?
    var GetStringUTFChars: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, CPointer<jbooleanVar>?) -> CPointer<ByteVar>?>>?
    var ReleaseFloatArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jfloatArray?, CPointer<jfloatVar>?, jint) -> Unit>>?
    var IsAssignableFrom: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jclass?) -> jboolean>>?
    var SetShortField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jshort) -> Unit>>?
    var CallByteMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jbyte>>?
    var ReleaseCharArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jcharArray?, CPointer<jcharVar>?, jint) -> Unit>>?
    var NewObjectArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize, jclass?, jobject?) -> jobjectArray?>>?
    var CallStaticIntMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jint>>?
    var GetObjectClass: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jclass?>>?
    var GetStringCritical: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, CPointer<jbooleanVar>?) -> CPointer<jcharVar>?>>?
    var CallStaticByteMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jbyte>>?
    var ReleaseDoubleArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jdoubleArray?, CPointer<jdoubleVar>?, jint) -> Unit>>?
    var GetStaticFieldID: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, CPointer<ByteVar>?, CPointer<ByteVar>?) -> jfieldID?>>?
    var SetBooleanArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbooleanArray?, jsize, jsize, CPointer<jbooleanVar>?) -> Unit>>?
    var reserved2: COpaquePointer?
    var NewStringUTF: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<ByteVar>?) -> jstring?>>?
    var SetObjectArrayElement: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobjectArray?, jsize, jobject?) -> Unit>>?
    var CallNonvirtualShortMethod: COpaquePointer?
    var GetCharArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jcharArray?, jsize, jsize, CPointer<jcharVar>?) -> Unit>>?
    var GetFloatArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jfloatArray?, CPointer<jbooleanVar>?) -> CPointer<jfloatVar>?>>?
    var CallObjectMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jobject?>>?
    var NewWeakGlobalRef: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jweak?>>?
    var CallCharMethod: COpaquePointer?
    var CallNonvirtualFloatMethod: COpaquePointer?
    var NewDoubleArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jdoubleArray?>>?
    var CallNonvirtualBooleanMethod: COpaquePointer?
    var SetStaticIntField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jint) -> Unit>>?
    var ToReflectedField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jboolean) -> jobject?>>?
    var GetStringRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, jsize, jsize, CPointer<jcharVar>?) -> Unit>>?
    var GetShortArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jshortArray?, jsize, jsize, CPointer<jshortVar>?) -> Unit>>?
    var MonitorExit: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jint>>?
    var CallStaticLongMethod: COpaquePointer?
    var CallShortMethod: COpaquePointer?
    var CallNonvirtualLongMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jlong>>?
    var GetMethodID: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, CPointer<ByteVar>?, CPointer<ByteVar>?) -> jmethodID?>>?
    var CallIntMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jint>>?
    var DefineClass: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<ByteVar>?, jobject?, CPointer<jbyteVar>?, jsize) -> jclass?>>?
    var GetStringChars: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, CPointer<jbooleanVar>?) -> CPointer<jcharVar>?>>?
    var GetIntArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jintArray?, jsize, jsize, CPointer<jintVar>?) -> Unit>>?
    var CallDoubleMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jmethodID?, CPointer<jvalue>?) -> jdouble>>?
    var CallStaticObjectMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jobject?>>?
    var ReleaseStringChars: CPointer<CFunction<(CPointer<JNIEnvVar>?, jstring?, CPointer<jcharVar>?) -> Unit>>?
    var GetStaticFloatField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?) -> jfloat>>?
    var GetIntArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jintArray?, CPointer<jbooleanVar>?) -> CPointer<jintVar>?>>?
    var CallStaticBooleanMethod: COpaquePointer?
    var GetBooleanArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jbooleanArray?, jsize, jsize, CPointer<jbooleanVar>?) -> Unit>>?
    var GetVersion: CPointer<CFunction<(CPointer<JNIEnvVar>?) -> jint>>?
    var CallStaticObjectMethod: COpaquePointer?
    var NewObjectA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jobject?>>?
    var SetStaticCharField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jchar) -> Unit>>?
    var IsSameObject: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jobject?) -> jboolean>>?
    var reserved3: COpaquePointer?
    var SetStaticBooleanField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jboolean) -> Unit>>?
    var SetStaticObjectField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jobject?) -> Unit>>?
    var CallStaticBooleanMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jboolean>>?
    var SetStaticFloatField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jfieldID?, jfloat) -> Unit>>?
    var CallStaticVoidMethod: COpaquePointer?
    var CallStaticCharMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jchar>>?
    var DeleteGlobalRef: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> Unit>>?
    var IsInstanceOf: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?) -> jboolean>>?
    var NewLocalRef: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?) -> jobject?>>?
    var CallStaticDoubleMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, jmethodID?, CPointer<jvalue>?) -> jdouble>>?
    var GetArrayLength: CPointer<CFunction<(CPointer<JNIEnvVar>?, jarray?) -> jsize>>?
    var GetLongArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jlongArray?, CPointer<jbooleanVar>?) -> CPointer<jlongVar>?>>?
    var ThrowNew: CPointer<CFunction<(CPointer<JNIEnvVar>?, jclass?, CPointer<ByteVar>?) -> jint>>?
    var CallDoubleMethod: COpaquePointer?
    var CallNonvirtualObjectMethod: COpaquePointer?
    var SetShortArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jshortArray?, jsize, jsize, CPointer<jshortVar>?) -> Unit>>?
    var NewIntArray: CPointer<CFunction<(CPointer<JNIEnvVar>?, jsize) -> jintArray?>>?
    var GetCharField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jchar>>?
    var reserved0: COpaquePointer?
    var GetLongField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jlong>>?
    var SetBooleanField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?, jboolean) -> Unit>>?
    var SetDoubleArrayRegion: CPointer<CFunction<(CPointer<JNIEnvVar>?, jdoubleArray?, jsize, jsize, CPointer<jdoubleVar>?) -> Unit>>?
    var GetDoubleField: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jfieldID?) -> jdouble>>?
    var ReleaseLongArrayElements: CPointer<CFunction<(CPointer<JNIEnvVar>?, jlongArray?, CPointer<jlongVar>?, jint) -> Unit>>?
    var CallNonvirtualBooleanMethodA: CPointer<CFunction<(CPointer<JNIEnvVar>?, jobject?, jclass?, jmethodID?, CPointer<jvalue>?) -> jboolean>>?


    var NewString: CPointer<CFunction<(CPointer<JNIEnvVar>?, CPointer<jcharVar>?, jsize) -> jstring?>>?

    companion object {
        operator fun invoke(platform: JNINativeInterface): CommonJNINativeInterface
    }
}