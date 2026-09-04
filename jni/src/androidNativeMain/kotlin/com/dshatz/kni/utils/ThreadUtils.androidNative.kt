package com.dshatz.kni.utils

import com.dshatz.kni.JNIEnv
import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.JavaVMVar
import com.dshatz.kni.binding.jint
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap.alloc
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import platform.android.JNI_OK

@OptIn(ExperimentalForeignApi::class)
actual fun CPointer<JavaVMVar>.AttachCurrentThread(
    envOut: CPointerVar<JNIEnvVar>
): jint {
    return pointed.pointed!!.AttachCurrentThread!!.invoke(this, envOut.ptr, null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun CPointer<JavaVMVar>.DetachCurrentThread(): jint {
    return pointed.pointed!!.DetachCurrentThread!!.invoke(this)
}