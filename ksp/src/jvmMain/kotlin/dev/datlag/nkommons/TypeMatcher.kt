package dev.datlag.nkommons

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.nio.ByteBuffer

internal object TypeMatcher {

    val UnitOrVoid = Unit::class.asTypeName()
    private val CPointer = ClassName("kotlinx.cinterop", "CPointer")
    private val JNIEnvVar = ClassName("dev.datlag.nkommons", "JNIEnvVar")
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
    val KDirectByteBuffer = ClassName("dev.datlag.nkommons", "ByteBuffer")
    val KCommonByteBuffer = ClassName("dev.datlag.nkommons", "CommonByteBuffer")

    private const val TYPE_BINDING_PACKAGE = "dev.datlag.nkommons.binding"
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
    val JValue = ClassName("dev.datlag.nkommons", "jvalue")

    object Method {

        val ToJBoolean = MemberName("dev.datlag.nkommons.utils", "toJBoolean")
        val ToJBooleanArray = MemberName("dev.datlag.nkommons.utils", "toJBooleanArray")
        val ToJByteArray = MemberName("dev.datlag.nkommons.utils", "toJByteArray")
        val ToJChar = MemberName("dev.datlag.nkommons.utils", "toJChar")
        val ToJCharArray = MemberName("dev.datlag.nkommons.utils", "toJCharArray")
        val ToJDoubleArray = MemberName("dev.datlag.nkommons.utils", "toJDoubleArray")
        val ToJFloatArray = MemberName("dev.datlag.nkommons.utils", "toJFloatArray")
        val ToJIntArray = MemberName("dev.datlag.nkommons.utils", "toJIntArray")
        val ToJLongArray = MemberName("dev.datlag.nkommons.utils", "toJLongArray")
        val ToJShortArray = MemberName("dev.datlag.nkommons.utils", "toJShortArray")
        val ToJString = MemberName("dev.datlag.nkommons.utils", "toJString")

        val ToJCommonByteBuffer = MemberName("dev.datlag.nkommons.utils", "toJCommonByteBuffer")

        val ToKBoolean = MemberName("dev.datlag.nkommons.utils", "toKBoolean")
        val ToKBooleanArray = MemberName("dev.datlag.nkommons.utils", "toKBooleanArray")
        val ToKByteArray = MemberName("dev.datlag.nkommons.utils", "toKByteArray")
        val ToKChar = MemberName("dev.datlag.nkommons.utils", "toKChar")
        val ToKCharArray = MemberName("dev.datlag.nkommons.utils", "toKCharArray")
        val ToKDoubleArray = MemberName("dev.datlag.nkommons.utils", "toKDoubleArray")
        val ToKFloatArray = MemberName("dev.datlag.nkommons.utils", "toKFloatArray")
        val ToKIntArray = MemberName("dev.datlag.nkommons.utils", "toKIntArray")
        val ToKLongArray = MemberName("dev.datlag.nkommons.utils", "toKLongArray")
        val ToKShortArray = MemberName("dev.datlag.nkommons.utils", "toKShortArray")
        val ToKString = MemberName("dev.datlag.nkommons.utils", "toKString")
        val ToKDirectByteBuffer = MemberName("dev.datlag.nkommons.utils", "toKDirectByteBuffer")
        val ToKCommonByteBuffer = MemberName("dev.datlag.nkommons.utils", "toKCommonByteBuffer")
    }

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
            KDirectByteBuffer -> if (forReturn) {
                JObject.copy(nullable = true)
            } else {
                JObject.copy(nullable = param.isNullable)
            }

            else -> JObject.copy(nullable = param.isNullable)
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