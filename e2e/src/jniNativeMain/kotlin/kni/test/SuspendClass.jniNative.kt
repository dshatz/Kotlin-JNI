package kni.test

import com.dshatz.kni.annotations.JniCall

actual class SuspendClass actual constructor(private val suspendCallback: SuspendingCallback): AutoCloseable {

    @JniCall
    actual suspend fun callToSuspendJvm(): ByteArray {
        println("callToSuspendJvm!")
        return suspendCallback.getBlock()
    }

    actual override fun close() {
    }
}