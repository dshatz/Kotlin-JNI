package kni.test

import com.dshatz.kni.annotations.JniCall

actual class SuspendClass actual constructor(private val suspendCallback: SuspendingCallback): AutoCloseable {

    @JniCall
    actual suspend fun callToSuspendJvm(): Result<ByteArray> {
        println("callToSuspendJvm!")
        return runCatching { suspendCallback.getBlock() }
    }

    @JniCall
    actual suspend fun doWork(): Result<Unit> {
        return Result.success(Unit)
    }

    actual override fun close() {
    }
}