package com.dshatz.kni.utils

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.JavaVMVar
import com.dshatz.kni.binding.JNI_EDETACHED
import com.dshatz.kni.binding.JNI_VERSION_1_6
import com.dshatz.kni.binding.jint
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value

@OptIn(ExperimentalForeignApi::class)
expect fun CPointer<JavaVMVar>.AttachCurrentThread(
    envOut: CPointerVar<JNIEnvVar>
): jint

@OptIn(ExperimentalForeignApi::class)
expect fun CPointer<JavaVMVar>.DetachCurrentThread(): jint

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JavaVMVar>.GetEnv(
    envOut: CPointerVar<JNIEnvVar>
): jint {
    return pointed.pointed?.GetEnv!!.invoke(this, envOut.ptr.reinterpret(), JNI_VERSION_1_6)
}

@OptIn(ExperimentalForeignApi::class)
fun <R> CPointer<JavaVMVar>.WithAttachedThread(block: (env: CPointer<JNIEnvVar>) -> R): R {
    memScoped {
        val envOut = alloc<CPointerVar<JNIEnvVar>>()
        val envResult = GetEnv(envOut)
        val needDetach = if (envResult == JNI_EDETACHED) {
            val attachResult = AttachCurrentThread(envOut)
            check(attachResult == 0) { "AttachCurrentThread failed with code $attachResult" }
            true
        } else false
        val envPointer = envOut.value
            ?: error("Failed to retrieve valid JNIEnv pointer")
        try {
            return block(envPointer)
        } finally {
            if (needDetach) DetachCurrentThread()
        }
    }
}