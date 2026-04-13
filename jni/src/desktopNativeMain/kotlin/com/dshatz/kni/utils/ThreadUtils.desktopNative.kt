package com.dshatz.kni.utils

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.JavaVMVar
import com.dshatz.kni.binding.jint
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual fun CPointer<JavaVMVar>.AttachCurrentThread(envOut: CPointerVar<JNIEnvVar>): jint {
    return pointed.pointed!!.AttachCurrentThread!!.invoke(
        this,
        envOut.ptr.reinterpret(),
        null
    )
}