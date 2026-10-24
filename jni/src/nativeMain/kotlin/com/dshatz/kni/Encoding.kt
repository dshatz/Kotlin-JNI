package com.dshatz.kni

import com.dshatz.kni.binding.jbooleanVar
import com.dshatz.kni.binding.jcharVar
import com.dshatz.kni.binding.jstring
import com.dshatz.kni.error.JniUnavailableError
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.wcstr

@OptIn(ExperimentalForeignApi::class)
sealed interface Encoding {

    fun String.toJString(env: CPointer<JNIEnvVar>): jstring?
    fun jstring.toKString(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): String

    data object UTF8 : Encoding {

        fun CPointer<JNIEnvVar>.newString(chars: CPointer<ByteVar>): jstring? {
            val method = pointed.pointedCommon?.NewStringUTF ?: return null
            return method.invoke(this, chars)
        }

        fun jstring.getChars(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): CPointer<ByteVar> {
            val method = env.pointed.pointedCommon?.GetStringUTFChars ?: throw JniUnavailableError()
            return method.invoke(env, this, isCopy) ?: throw OutOfMemoryError("Failed to call GetStringUTFChars")
        }

        override fun String.toJString(env: CPointer<JNIEnvVar>): jstring? {
            return ModifiedUtf8.encode(this, nullTerminated = true).usePinned {
                env.newString(it.addressOf(0))
            }
        }

        fun jstring.releaseChars(env: CPointer<JNIEnvVar>, chars: CPointer<ByteVar>) {
            val method = env.pointed.pointedCommon?.ReleaseStringUTFChars ?: throw JniUnavailableError()
            method.invoke(env, this, chars)
        }

        override fun jstring.toKString(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>?): String {
            val common = env.pointed.pointedCommon ?: throw JniUnavailableError()
            val length = common.GetStringLength!!.invoke(env, this)
            if (length == 0) return ""
            val byteLength = common.GetStringUTFLength!!.invoke(env, this)
            // +1: some JVMs write a terminating zero byte.
            val bytes = ByteArray(byteLength + 1)
            bytes.usePinned {
                common.GetStringUTFRegion!!.invoke(env, this, 0, length, it.addressOf(0))
            }
            return ModifiedUtf8.decode(bytes, byteLength)
        }
    }

    data object UTF16 : Encoding {

        fun CPointer<JNIEnvVar>.newString(chars: CPointer<jcharVar>, length: Int): jstring? {
            val method = pointed.pointedCommon?.NewString ?: return null
            return method.invoke(this, chars, length)
        }

        fun jstring.getChars(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>? = null): CPointer<jcharVar> {
            val method = env.pointed.pointedCommon?.GetStringChars ?: throw JniUnavailableError()
            return method.invoke(env, this, isCopy) ?: throw OutOfMemoryError("Failed to call GetStringChars")
        }

        override fun String.toJString(env: CPointer<JNIEnvVar>): jstring? = memScoped {
            env.newString(wcstr.ptr, length)
        }

        fun jstring.releaseChars(env: CPointer<JNIEnvVar>, chars: CPointer<jcharVar>) {
            val method = env.pointed.pointedCommon?.ReleaseStringChars ?: throw JniUnavailableError()
            method.invoke(env, this, chars)
        }

        override fun jstring.toKString(env: CPointer<JNIEnvVar>, isCopy: CPointer<jbooleanVar>?): String {
            val common = env.pointed.pointedCommon ?: throw JniUnavailableError()
            val length = common.GetStringLength!!.invoke(env, this)
            if (length == 0) return ""
            val chars = CharArray(length)
            chars.usePinned {
                common.GetStringRegion!!.invoke(env, this, 0, length, it.addressOf(0).reinterpret())
            }
            return chars.concatToString()
        }

    }
}