package kni.test

import com.dshatz.kni.annotations.JniCall

expect class SuspendClass(
    suspendCallback: SuspendingCallback
): AutoCloseable {

    @JniCall
    suspend fun callToSuspendJvm(): ByteArray

    override fun close()

}