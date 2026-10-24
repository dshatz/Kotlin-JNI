package kni.test

import com.dshatz.kni.Encoding
import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.annotations.JniAdapter
import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.binding.jstring
import com.dshatz.kni.utils.toJString
import com.dshatz.kni.utils.toKString
import com.dshatz.kni.wrapper.NativeJniAdapter
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

const val NATIVE_TEXT = "native 😀 日本語"

@JniAdapter(Utf8TextAdapter::class)
actual class Utf8Text actual constructor(actual val value: String)

@OptIn(ExperimentalForeignApi::class)
actual object Utf8TextAdapter : NativeJniAdapter<Utf8Text, jstring> {
    override fun fromJni(env: CPointer<JNIEnvVar>, value: jstring): Utf8Text =
        Utf8Text(value.toKString(env, Encoding.UTF8))

    override fun toJni(env: CPointer<JNIEnvVar>, value: Utf8Text): jstring =
        value.value.toJString(env, Encoding.UTF8)
}

@JniAdapter(Utf16TextAdapter::class)
actual class Utf16Text actual constructor(actual val value: String)

@OptIn(ExperimentalForeignApi::class)
actual object Utf16TextAdapter : NativeJniAdapter<Utf16Text, jstring> {
    override fun fromJni(env: CPointer<JNIEnvVar>, value: jstring): Utf16Text =
        Utf16Text(value.toKString(env, Encoding.UTF16))

    override fun toJni(env: CPointer<JNIEnvVar>, value: Utf16Text): jstring =
        value.value.toJString(env, Encoding.UTF16)
}

actual object EncodingBridge {
    @JniCall actual fun echoUtf8(text: Utf8Text): Utf8Text = text
    @JniCall actual fun echoUtf16(text: Utf16Text): Utf16Text = text

    @JniCall actual fun lengthUtf8(text: Utf8Text): Int = text.value.length
    @JniCall actual fun lengthUtf16(text: Utf16Text): Int = text.value.length

    @JniCall actual fun fromNativeUtf8(): Utf8Text = Utf8Text(NATIVE_TEXT)
    @JniCall actual fun fromNativeUtf16(): Utf16Text = Utf16Text(NATIVE_TEXT)
}
