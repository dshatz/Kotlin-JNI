package dev.datlag.nkommons

import dev.datlag.nkommons.binding.jobject
import dev.datlag.nkommons.utils.CallVoidMethodA
import dev.datlag.nkommons.utils.DeleteGlobalRef
import dev.datlag.nkommons.utils.DeleteLocalRef
import dev.datlag.nkommons.utils.FindClass
import dev.datlag.nkommons.utils.GetMethodID
import dev.datlag.nkommons.utils.NewGlobalRef
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed

@OptIn(ExperimentalForeignApi::class)
open class BaseCallback(
    val env: CPointer<JNIEnvVar>,
    private val className: String,
    instance: jobject
): AutoCloseable {

    public val jvmClass: jobject
        get() {
            if (isClosed) error("This native instance of ${this.className} has been closed.")
            return env.FindClass(className)!!
        }

    private var isClosed: Boolean = false

    val ref: jobject = env.NewGlobalRef(instance) ?: error("Unable to create new GlobalRef")

    private fun callCloseOnJvm() {
        val cls = jvmClass
        val methodId = env.GetMethodID(cls, "close", "()V") ?: error("close method not found")
        memScoped {
            val args = allocArray<jvalue>(0)
            env.CallVoidMethodA(ref, methodId, args)
        }
    }

    override fun close() {
        // Signal to JVM that it should clear the resources.
        runCatching {
            callCloseOnJvm()
        }.onFailure { it.printStackTrace() }

        runCatching {
            // Get rid of the global ref.
            env.DeleteGlobalRef(ref)
            isClosed = true
        }.onFailure {
            it.printStackTrace()
        }
    }
}