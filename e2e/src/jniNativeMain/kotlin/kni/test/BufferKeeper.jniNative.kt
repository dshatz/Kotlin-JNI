package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.buffers.BufferReleasedException
import com.dshatz.kni.buffers.ByteBuffer
import kotlinx.coroutines.delay
import kotlin.concurrent.Volatile

actual object BufferKeeper {
    // Written from suspend functions, which run on another thread.
    @Volatile private var borrowed: ByteBuffer? = null
    @Volatile private var retained: ByteBuffer? = null

    @JniCall
    actual fun storeBorrowed(buffer: ByteBuffer) {
        borrowed = buffer
    }

    @JniCall
    actual suspend fun storeBorrowedSuspend(buffer: ByteBuffer) {
        borrowed = buffer
    }

    @JniCall
    actual fun isStoredInvalidated(): Boolean {
        val buffer = borrowed ?: error("storeBorrowed was not called")
        if (!buffer.isReleased) return false
        return try {
            buffer.write(byteArrayOf(1))
            false
        } catch (_: BufferReleasedException) {
            true
        }
    }

    @JniCall
    actual fun storeRetained(buffer: ByteBuffer) {
        retained = buffer.retain()
    }

    @JniCall
    actual suspend fun storeRetainedSuspend(buffer: ByteBuffer) {
        retained = buffer.retain()
    }

    @JniCall
    actual fun writeToRetained(value: Byte) {
        retained!!.write(byteArrayOf(value))
    }

    @JniCall
    actual fun releaseRetained() {
        retained!!.release()
        retained = null
    }

    @JniCall
    actual fun nullableBuffer(buffer: ByteBuffer?): Long {
        return buffer?.capacity ?: -1
    }

    @JniCall
    actual suspend fun nullableBufferSuspend(buffer: ByteBuffer?): Long {
        return buffer?.capacity ?: -1
    }

    @JniCall
    actual suspend fun fillAfterDelay(buffer: ByteBuffer, value: Byte, delayMillis: Long) {
        delay(delayMillis)
        buffer.write(ByteArray(buffer.capacity.toInt()) { value })
    }

    @JniCall
    actual fun readFromProvider(provider: BufferProvider): Byte {
        return provider.use { it.provide().readFirstAndRelease() }
    }

    @JniCall
    actual suspend fun readFromSuspendProvider(provider: BufferProvider): Byte {
        return provider.use { it.provideSuspend().readFirstAndRelease() }
    }

    private fun ByteBuffer.readFirstAndRelease(): Byte {
        try {
            return readToByteArray(0, 1)[0]
        } finally {
            release()
        }
    }
}
