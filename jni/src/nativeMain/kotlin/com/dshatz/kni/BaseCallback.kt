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
import com.dshatz.kni.utils.ExceptionCheck
import com.dshatz.kni.utils.ExceptionClear
import com.dshatz.kni.utils.FindClass
import com.dshatz.kni.utils.GetMethodID
import com.dshatz.kni.utils.GetStaticMethodID
import com.dshatz.kni.utils.NewGlobalRef
import com.dshatz.kni.utils.getJavaVM
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
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
import kotlin.concurrent.Volatile
import kotlin.native.concurrent.ThreadLocal

@OptIn(ExperimentalForeignApi::class)
open class BaseCallback(
    private val className: String,
    env: CPointer<JNIEnvVar>,
    instance: jobject
): AutoCloseable {

    protected val lock = ReentrantLock()

    @Volatile
    var isClosed: Boolean = false
        protected set

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

    val ref: jobject = env.NewGlobalRef(instance)

    protected inline fun <T> runIfOpen(block: () -> T): T {
        return lock.withLock {
            if (isClosed) error("${this::class.simpleName} callback is closed.") else block()
        }
    }

    protected fun lazyMethodId(name: String, signature: String): Lazy<jmethodID> {
        return lazy {
            val params = signature.substringAfter('(').substringBeforeLast(')')
            val instanceParam = "L${className}"
            val returnType = signature.substringAfterLast(')')
            val adapterSignature = "($instanceParam;$params)$returnType"
            env.GetStaticMethodID(adapterClassGlobal, name, adapterSignature) ?: error("$name method not found $adapterSignature")
        }
    }

    private fun callCloseOnJvm() {
        val closeCall = env.GetMethodID(jvmClassGlobal, "close", "()V")
        if (env.ExceptionCheck() == 1.toUByte()) {
            env.ExceptionClear()
            return
        }
        if (closeCall != null) {
            memScoped {
                val args = allocArray<jvalue>(0)
                runCatching {
                    env.CallVoidMethodA(ref, closeCall, args)
                }
            }
        }
    }

    override fun close() {
        lock.withLock {
            if (isClosed) return@withLock
            isClosed = true

            runCatching {
                callCloseOnJvm()
            }.onFailure { it.printStackTrace() }

            runCatching {
                env.DeleteGlobalRef(ref)
                env.DeleteGlobalRef(jvmClassGlobal)
                env.DeleteGlobalRef(adapterClassGlobal)
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@ThreadLocal
private var envCache: CPointer<JNIEnvVar>? = null