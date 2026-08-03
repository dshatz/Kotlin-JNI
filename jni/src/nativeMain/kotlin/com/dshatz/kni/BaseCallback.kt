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
import com.dshatz.kni.utils.GetStaticMethodID
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

    val env: CPointer<JNIEnvVar> get() {
        envCache?.let { return it }

        val nativeEnvPtr = nativeHeap.alloc<CPointerVar<JNIEnvVar>>()
        try {
            val vm = javaVm.pointed.pointed ?: error("JavaVM pointer is null")
            val result = vm.GetEnv!!.invoke(javaVm, nativeEnvPtr.ptr.reinterpret(), JNI_VERSION_1_6)

            if (result == JNI_EDETACHED) {
                if (javaVm.AttachCurrentThread(nativeEnvPtr) != JNI_OK) {
                    error("Failed to attach current thread")
                }
            }

            val envInstance = nativeEnvPtr.value ?: error("Failed to obtain JNIEnv for current thread")
            envCache = envInstance
            return envInstance
        } finally {
            nativeHeap.free(nativeEnvPtr)
        }
    }

    private val jvmClassGlobal: jobject = env.run {
        val localClass = FindClass(className) ?: error("Class not found: $className")
        val globalClass = NewGlobalRef(localClass) ?: error("Failed to create GlobalRef")
        DeleteLocalRef(localClass)
        globalClass
    }

    protected val adapterClassGlobal: jobject = env.run {
        val name = "${className}Adapter"
        val localClass = FindClass(name) ?: error("Class not found: $name")
        val globalClass = NewGlobalRef(localClass) ?: error("Failed to create GlobalRef for adapter class")
        DeleteLocalRef(localClass)
        globalClass
    }

    private var isClosed: Boolean = false

    val ref: jobject = env.NewGlobalRef(instance) ?: error("Unable to create new GlobalRef")

    protected fun lazyMethodId(name: String, signature: String): Lazy<jmethodID> {
        return lazy {
            val params = signature.substringAfter('(').substringBeforeLast(')')
            val instanceParam = "L${className}"
            val returnType = signature.substringAfterLast(')')
            val adapterSignature = "($instanceParam;$params)$returnType"
            env.GetStaticMethodID(adapterClassGlobal, name, adapterSignature) ?: error("$name method not found $adapterSignature")
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
        runCatching {
            callCloseOnJvm()
        }.onFailure { it.printStackTrace() }

        runCatching {
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