package com.dshatz.kni

import com.dshatz.kni.binding.jbooleanVar
import com.dshatz.kni.binding.jcharVar
import com.dshatz.kni.binding.jstring
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.wcstr

@OptIn(ExperimentalForeignApi::class)
sealed interface Encoding {

    fun String.toJString(env: CPointer<JNIEnvVar>): jstring?
    fun jstring.toKString(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): String?

    data object UTF8 : Encoding {

        fun CPointer<JNIEnvVar>.newString(chars: CPointer<ByteVar>): jstring? {
            val method = pointed.pointedCommon?.NewStringUTF ?: return null
            return method.invoke(this, chars)
        }

        fun jstring.getChars(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): CPointer<ByteVar>? {
            val method = env.pointed.pointedCommon?.GetStringUTFChars ?: return null
            return method.invoke(env, this, isCopy)
        }

        override fun String.toJString(env: CPointer<JNIEnvVar>): jstring? = memScoped {
            env.newString(cstr.ptr)
        }

        override fun jstring.toKString(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>?): String? {
            val chars = getChars(env, isCopy)
            return chars?.toKStringFromUtf8()
        }
    }

    data object UTF16 : Encoding {

        fun CPointer<JNIEnvVar>.newString(chars: CPointer<jcharVar>, length: Int): jstring? {
            val method = pointed.pointedCommon?.NewString ?: return null
            return method.invoke(this, chars, length)
        }

        fun jstring.getChars(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): CPointer<jcharVar>? {
            val method = env.pointed.pointedCommon?.GetStringChars ?: return null
            return method.invoke(env, this, isCopy)
        }

        override fun String.toJString(env: CPointer<JNIEnvVar>): jstring? = memScoped {
            env.newString(wcstr.ptr, length)
        }

        override fun jstring.toKString(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>?): String? {
            val chars = getChars(env, isCopy)
            return chars?.toKStringFromUtf16()
        }

    }
}