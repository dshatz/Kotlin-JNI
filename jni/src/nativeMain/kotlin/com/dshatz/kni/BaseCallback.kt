package com.dshatz.kni

import com.dshatz.kni.binding.JNI_EDETACHED
import com.dshatz.kni.binding.JNI_OK
import com.dshatz.kni.binding.JNI_VERSION_1_6
import com.dshatz.kni.binding.jmethodID
import com.dshatz.kni.binding.jobject
import com.dshatz.kni.utils.AttachCurrentThread
import com.dshatz.kni.utils.CallVoidMethodA
import com.dshatz.kni.utils.DeleteGlobalRef
import com.dshatz.kni.utils.DeleteLocalRef
import com.dshatz.kni.utils.FindClass
import com.dshatz.kni.utils.GetMethodID
import com.dshatz.kni.utils.NewGlobalRef
import com.dshatz.kni.utils.getJavaVM
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.free
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import kotlin.native.concurrent.ThreadLocal

@OptIn(ExperimentalForeignApi::class)
open class BaseCallback(
    private val className: String,
    env: CPointer<JNIEnvVar>,
    instance: jobject
): AutoCloseable {

    private val javaVm: CPointer<JavaVMVar> = env.getJavaVM()
    private var envPtr: CPointer<CPointerVar<JNIEnvVar>>? = nativeHeap.alloc<CPointerVar<JNIEnvVar>>().ptr

    val env: CPointer<JNIEnvVar> get() {
        return envCache ?: run {
            println("Env lazy block")
            val p = envPtr ?: error("Callback already closed")
            val vm = javaVm.pointed.pointed!!
            val result = vm.GetEnv!!.invoke(javaVm, p.reinterpret(), JNI_VERSION_1_6)

            if (result == JNI_EDETACHED) {
                println("JNI_EDETACHED, Attaching")

                if (javaVm.AttachCurrentThread(p.pointed) != JNI_OK) {
                    println("Attaching failed")
                }
            }
            println("env retrieved")

            p.pointed.value!!
        }.also { envCache = it }
    }

    private val jvmClassGlobal: jobject = env.run {
        val localClass = FindClass(className) ?: error("Class not found: $className")
        val globalClass = NewGlobalRef(localClass) ?: error("Failed to create GlobalRef")
        DeleteLocalRef(localClass)
        globalClass
    }

    private var isClosed: Boolean = false

    val ref: jobject = env.NewGlobalRef(instance) ?: error("Unable to create new GlobalRef")

    fun lazyMethodId(name: String, signature: String): Lazy<jmethodID> {
        return lazy {
            env.GetMethodID(jvmClassGlobal, name, signature) ?: error("$name method not found $signature")
        }
    }


    val closeMethod by lazyMethodId("close", "()V")
    private fun callCloseOnJvm() {
        memScoped {
            val args = allocArray<jvalue>(0)
            env.CallVoidMethodA(ref, closeMethod, args)
        }
    }

    override fun close() {
        // Signal to JVM that it should clear the resources.
        runCatching {
            callCloseOnJvm()
        }.onFailure { it.printStackTrace() }

        envPtr?.let { nativeHeap.free(it); envPtr = null }

        runCatching {
            // Get rid of the global ref.
            env.DeleteGlobalRef(ref)
            env.DeleteGlobalRef(jvmClassGlobal)
            isClosed = true
        }.onFailure {
            it.printStackTrace()
        }
    }
}


@OptIn(ExperimentalForeignApi::class)
@ThreadLocal
private var envCache: CPointer<JNIEnvVar>? = null