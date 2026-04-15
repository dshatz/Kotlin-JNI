package com.dshatz.kni.utils

import com.dshatz.kni.Encoding
import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.binding.jbooleanVar
import com.dshatz.kni.binding.jstring
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun String.toJString(env: CPointer<JNIEnvVar>, encoding: Encoding = Encoding.UTF8): jstring? {
    return with(encoding) {
        this@toJString.toJString(env)
    }
}

@OptIn(ExperimentalForeignApi::class)
fun jstring.toKString(
    env: CPointer<JNIEnvVar>,
    encoding: Encoding = Encoding.UTF8,
    isCopy: CPointer<jbooleanVar>? = null
): String? {
    return with(encoding) {
        this@toKString.toKString(env, isCopy)
    }
}