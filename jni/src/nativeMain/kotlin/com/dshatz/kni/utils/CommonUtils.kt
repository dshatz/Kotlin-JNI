package com.dshatz.kni.utils

import com.dshatz.kni.CommonJNINativeInterface
import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.JNINativeInterface
import com.dshatz.kni.JavaVMVar
import com.dshatz.kni.binding.JNI_EDETACHED
import com.dshatz.kni.binding.JNI_OK
import com.dshatz.kni.binding.JNI_VERSION_1_6
import com.dshatz.kni.binding.jarray
import com.dshatz.kni.error.JniUnavailableError
import com.dshatz.kni.pointedCommon
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value

val JNINativeInterface.common
    get() = CommonJNINativeInterface.Companion(this)

@OptIn(ExperimentalForeignApi::class)
fun jarray.getLength(env: CPointer<JNIEnvVar>): Int {
    val method = env.pointed.pointedCommon?.GetArrayLength ?: throw JniUnavailableError()
    return method.invoke(env, this)
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.getJavaVM(): CPointer<JavaVMVar> {
    val vmPtr = nativeHeap.alloc<CPointerVar<JavaVMVar>>()
    pointed.pointedCommon!!.GetJavaVM!!.invoke(this, vmPtr.ptr)
    val vm = vmPtr.value!!
    nativeHeap.free(vmPtr)
    return vm
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<JNIEnvVar>.GetAndAttach(): CPointer<JNIEnvVar>? {
    val vm = getJavaVM()
    memScoped {
        val envOut = alloc<CPointerVar<JNIEnvVar>>()
        val result = vm.AttachCurrentThread(envOut = envOut)

        if (result != JNI_OK) {
            error("Failed to attach thread: $result")
        }
        return envOut.value
    }
}
