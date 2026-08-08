package kni.test

import com.dshatz.kni.annotations.JniCall

expect class SuspendClass(
    suspendCallback: SuspendingCallback
): AutoCloseable {

    @JniCall
    suspend fun callToSuspendJvm(): Result<ByteArray>

    @JniCall
    suspend fun doWork(): Result<Unit>

    override fun close()

}