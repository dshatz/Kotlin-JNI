package com.dshatz.kni

import com.dshatz.kni.annotations.KniInternalApi
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asTypeName

internal object Types {

    val UnitOrVoid = Unit::class.asTypeName()
    private val CPointer = ClassName("kotlinx.cinterop", "CPointer")
    private val JNIEnvVar = ClassName("com.dshatz.kni", "JNIEnvVar")
    val Environment = CPointer.parameterizedBy(JNIEnvVar)

    val KBoolean = Boolean::class.asTypeName()
    val KBooleanArray = BooleanArray::class.asTypeName()
    val KByte = Byte::class.asTypeName()
    val KByteArray = ByteArray::class.asTypeName()
    val KChar = Char::class.asTypeName()
    val KCharArray = CharArray::class.asTypeName()
    val KDouble = Double::class.asTypeName()
    val KDoubleArray = DoubleArray::class.asTypeName()
    val KFloat = Float::class.asTypeName()
    val KFloatArray = FloatArray::class.asTypeName()
    val KInt = Int::class.asTypeName()
    val KIntArray = IntArray::class.asTypeName()
    val KLong = Long::class.asTypeName()
    val KLongArray = LongArray::class.asTypeName()
    val KShort = Short::class.asTypeName()
    val KShortArray = ShortArray::class.asTypeName()
    val KString = String::class.asTypeName()
    private const val TYPE_BINDING_PACKAGE = "com.dshatz.kni.binding"

    val KByteBuffer = ClassName("com.dshatz.kni.buffers", "ByteBuffer")
    val KNioBuffer = ClassName("java.nio", "ByteBuffer")
    val JBoolean = ClassName(TYPE_BINDING_PACKAGE, "jboolean")
    val JBooleanArray = ClassName(TYPE_BINDING_PACKAGE, "jbooleanArray")
    val JByte = ClassName(TYPE_BINDING_PACKAGE, "jbyte")
    val JByteArray = ClassName(TYPE_BINDING_PACKAGE, "jbyteArray")
    val JChar = ClassName(TYPE_BINDING_PACKAGE, "jchar")
    val JCharArray = ClassName(TYPE_BINDING_PACKAGE, "jcharArray")
    val JDouble = ClassName(TYPE_BINDING_PACKAGE, "jdouble")
    val JDoubleArray = ClassName(TYPE_BINDING_PACKAGE, "jdoubleArray")
    val JFloat = ClassName(TYPE_BINDING_PACKAGE, "jfloat")
    val JFloatArray = ClassName(TYPE_BINDING_PACKAGE, "jfloatArray")
    val JInt = ClassName(TYPE_BINDING_PACKAGE, "jint")
    val JIntArray = ClassName(TYPE_BINDING_PACKAGE, "jintArray")
    val JLong = ClassName(TYPE_BINDING_PACKAGE, "jlong")
    val JLongArray = ClassName(TYPE_BINDING_PACKAGE, "jlongArray")
    val JShort = ClassName(TYPE_BINDING_PACKAGE, "jshort")
    val JShortArray = ClassName(TYPE_BINDING_PACKAGE, "jshortArray")
    val JString = ClassName(TYPE_BINDING_PACKAGE, "jstring")
    val JObject = ClassName(TYPE_BINDING_PACKAGE, "jobject")
    val JValue = ClassName("com.dshatz.kni", "jvalue")
    val JMethodID = ClassName("com.dshatz.kni.binding", "jmethodID")
    val BaseCallback = ClassName("com.dshatz.kni", "BaseCallback")
    val AutoCloseable = ClassName("kotlin", "AutoCloseable")
    val IoBuffer = ClassName("kotlinx.io", "Buffer")

    val JniSerializer = ClassName("com.dshatz.kni.serialization", "JniSerializer")

    val NativeBackedFlow = ClassName("com.dshatz.kni.flows", "NativeBackedFlow")
    val FlowCallback = ClassName("com.dshatz.kni.flows", "FlowCallback")

    val NativeInstanceJvm = ClassName("com.dshatz.kni", "NativeInstanceJvm")
    val SuspendCallback = ClassName("com.dshatz.kni.flows", "SuspendCallback")
    val SuspendCallback0 = ClassName("com.dshatz.kni.flows", "SuspendCallback0")
    val SuspendCallbackImpl = ClassName("com.dshatz.kni.flows", "SuspendCallbackImpl")
    val SuspendCallbackImpl0 = ClassName("com.dshatz.kni.flows", "SuspendCallbackImpl0")

    val AtomicLong = ClassName("kotlin.concurrent.atomics", "AtomicLong")


    object Method {

        val ToJBoolean = MemberName("com.dshatz.kni.utils", "toJBoolean")
        val ToJBooleanArray = MemberName("com.dshatz.kni.utils", "toJBooleanArray")
        val ToJByteArray = MemberName("com.dshatz.kni.utils", "toJByteArray")
        val ToJChar = MemberName("com.dshatz.kni.utils", "toJChar")
        val ToJCharArray = MemberName("com.dshatz.kni.utils", "toJCharArray")
        val ToJDoubleArray = MemberName("com.dshatz.kni.utils", "toJDoubleArray")
        val ToJFloatArray = MemberName("com.dshatz.kni.utils", "toJFloatArray")
        val ToJIntArray = MemberName("com.dshatz.kni.utils", "toJIntArray")
        val ToJLongArray = MemberName("com.dshatz.kni.utils", "toJLongArray")
        val ToJShortArray = MemberName("com.dshatz.kni.utils", "toJShortArray")
        val ToJString = MemberName("com.dshatz.kni.utils", "toJString")

        val ToJByteBuffer = MemberName("com.dshatz.kni.utils", "toJByteBuffer")
        val ToJNioByteBuffer = MemberName("com.dshatz.kni.utils", "toJNioByteBuffer")

        val ToKBoolean = MemberName("com.dshatz.kni.utils", "toKBoolean")
        val ToKBooleanArray = MemberName("com.dshatz.kni.utils", "toKBooleanArray")
        val ToKByteArray = MemberName("com.dshatz.kni.utils", "toKByteArray")
        val ToKChar = MemberName("com.dshatz.kni.utils", "toKChar")
        val ToKCharArray = MemberName("com.dshatz.kni.utils", "toKCharArray")
        val ToKDoubleArray = MemberName("com.dshatz.kni.utils", "toKDoubleArray")
        val ToKFloatArray = MemberName("com.dshatz.kni.utils", "toKFloatArray")
        val ToKIntArray = MemberName("com.dshatz.kni.utils", "toKIntArray")
        val ToKLongArray = MemberName("com.dshatz.kni.utils", "toKLongArray")
        val ToKShortArray = MemberName("com.dshatz.kni.utils", "toKShortArray")
        val ToKString = MemberName("com.dshatz.kni.utils", "toKString")
        val ToKDirectByteBuffer = MemberName("com.dshatz.kni.utils", "toKDirectByteBuffer")
        val ToKByteBuffer = MemberName("com.dshatz.kni.buffers", "toKByteBuffer")
        val GetAndAttach = MemberName("com.dshatz.kni.utils", "GetAndAttach")
        val Pack = MemberName("com.dshatz.kni.serialization", "pack")
        val Unpack = MemberName("com.dshatz.kni.serialization", "unpack")

        val asStableRefLongPointer = MemberName("com.dshatz.kni.utils", "asStableRefLongPointer")
        val stableRefFromPointer = MemberName("com.dshatz.kni.utils", "stableRefFromPointer")
        val valueFromStableRefPointer = MemberName("com.dshatz.kni.utils", "valueFromStableRefPointer")
        val FromLongPointer = MemberName("com.dshatz.kni.utils", "fromLongPointer")
        val ReleaseStableRef = MemberName("com.dshatz.kni.utils", "releaseStableRef")

        val Serialize = MemberName("com.dshatz.kni.serialization", "serialize")
        val Deserialize = MemberName("com.dshatz.kni.serialization", "deserialize")
        val SuspendCancellableCoroutine = MemberName("kotlinx.coroutines", "suspendCancellableCoroutine")
        val Resume = MemberName("kotlin.coroutines", "resume")
        val ExecuteSuspend = MemberName("com.dshatz.kni.flows", "executeSuspend")
    }

    object Annotations {
        val CName = ClassName("kotlin.native", "CName")
        object Optin {
            private val OptIn = ClassName("kotlin", "OptIn")
            private val ExperimentalForeignApi = ClassName("kotlinx.cinterop", "ExperimentalForeignApi")
            private val ExperimentalNativeApi = ClassName("kotlin.experimental", "ExperimentalNativeApi")

            private val ExperimentalAtomicApi = ClassName("kotlin.concurrent.atomics", "ExperimentalAtomicApi")

            val NativeOptIn = AnnotationSpec.builder(OptIn)
                .addMember("%T::class, %T::class", ExperimentalForeignApi, ExperimentalNativeApi)
                .build()

            val KniInternalOptIn = AnnotationSpec.builder(OptIn)
                .addMember("%T::class", KniInternalApi::class)
                .build()

            val AtomicsOptIn = AnnotationSpec.builder(OptIn)
                .addMember("%T::class", ExperimentalAtomicApi)
                .build()
        }
    }

    val jTypes = mapOf(
        KBoolean to JBoolean,
        KBooleanArray to JBooleanArray,
        KByte to JByte,
        KByteArray to JByteArray,
        KChar to JChar,
        KCharArray to JCharArray,
        KDouble to JDouble,
        KDoubleArray to JDoubleArray,
        KFloat to JFloat,
        KFloatArray to JFloatArray,
        KInt to JInt,
        KIntArray to JIntArray,
        KLong to JLong,
        KLongArray to JLongArray,
        KShort to JShort,
        KShortArray to JShortArray,
        KString to JString,
    )

    val jniFields = mapOf(
        KBoolean to "z",
        KByte to "b",
        KChar to "c",
        KShort to "s",
        KInt to "i",
        KLong to "j",
        KFloat to "f",
        KDouble to "d"
    )

    val conversionWithoutEnv = listOf(
        KBoolean,
        KChar
    )

    val boxedWhenNullable = mapOf(
        KInt to CodeBlock.of("0"),
        KBoolean to CodeBlock.of("false"),
        KChar to CodeBlock.of("'a'"),
        KLong to CodeBlock.of("0l"),
        KFloat to CodeBlock.of("0f"),
        KDouble to CodeBlock.of("0.0"),
        KByte to CodeBlock.of("0"),
        KShort to CodeBlock.of("0")
    )

    val toJTypes = mapOf(
        KBoolean to Method.ToJBoolean,
        KBooleanArray to Method.ToJBooleanArray,
        KByteArray to Method.ToJByteArray,
        KChar to Method.ToJChar,
        KCharArray to Method.ToJCharArray,
        KDoubleArray to Method.ToJDoubleArray,
        KFloatArray to Method.ToJFloatArray,
        KIntArray to Method.ToJIntArray,
        KLongArray to Method.ToJLongArray,
        KShortArray to Method.ToJShortArray,
        KString to Method.ToJString
    )

    val toKTypes = mapOf(
        JBoolean to Method.ToKBoolean,
        JBooleanArray to Method.ToKBooleanArray,
        JByteArray to Method.ToKByteArray,
        JChar to Method.ToKChar,
        JCharArray to Method.ToKCharArray,
        JDoubleArray to Method.ToKDoubleArray,
        JFloatArray to Method.ToKFloatArray,
        JIntArray to Method.ToKIntArray,
        JLongArray to Method.ToKLongArray,
        JShortArray to Method.ToKShortArray,
        JString to Method.ToKString
    )

    fun jniTypeFor(param: TypeName, forReturn: Boolean): TypeName? {
        return when (param) {
            KBoolean -> JBoolean.copy(nullable = param.isNullable)
            KBooleanArray -> if (forReturn) {
                JBooleanArray.copy(nullable = true)
            } else {
                JBooleanArray.copy(nullable = param.isNullable)
            }
            KByte -> JByte.copy(nullable = param.isNullable)
            KByteArray -> if (forReturn) {
                JByteArray.copy(nullable = true)
            } else {
                JByteArray.copy(nullable = param.isNullable)
            }
            KChar -> JChar.copy(nullable = param.isNullable)
            KCharArray -> if (forReturn) {
                JCharArray.copy(nullable = true)
            } else {
                JCharArray.copy(nullable = param.isNullable)
            }
            KDouble -> JDouble.copy(nullable = param.isNullable)
            KDoubleArray -> if (forReturn) {
                JDoubleArray.copy(nullable = true)
            } else {
                JDoubleArray.copy(nullable = param.isNullable)
            }
            KFloat -> JFloat.copy(nullable = param.isNullable)
            KFloatArray -> if (forReturn) {
                JFloatArray.copy(nullable = true)
            } else {
                JFloatArray.copy(nullable = param.isNullable)
            }
            KInt -> JInt.copy(nullable = param.isNullable)
            KIntArray -> if (forReturn) {
                JIntArray.copy(nullable = true)
            } else {
                JIntArray.copy(nullable = param.isNullable)
            }
            KLong -> JLong.copy(nullable = param.isNullable)
            KLongArray -> if (forReturn) {
                JLongArray.copy(nullable = true)
            } else {
                JLongArray.copy(nullable = param.isNullable)
            }
            KShort -> JShort.copy(nullable = param.isNullable)
            KShortArray -> if (forReturn) {
                JShortArray.copy(nullable = true)
            } else {
                JShortArray.copy(nullable = param.isNullable)
            }
            KString -> if (forReturn) {
                JString.copy(nullable = true)
            } else {
                JString.copy(nullable = param.isNullable)
            }
            KByteBuffer -> if (forReturn) {
                JObject.copy(nullable = true)
            } else {
                JObject.copy(nullable = param.isNullable)
            }
            UNIT -> UNIT

            else -> null
        }
    }

    infix fun TypeName?.typeOf(expected: TypeName): Boolean = when {
        this == null -> false
        this == expected -> true
        this.copy(nullable = false) == expected.copy(nullable = false) -> true
        this.copy(nullable = true) == expected.copy(nullable = true) -> true
        else -> false
    }

}