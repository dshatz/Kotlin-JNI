package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.buffers.BufferReleasedException
import com.dshatz.kni.buffers.ByteBuffer

actual object BufferKeeper {
    private var borrowed: ByteBuffer? = null
    private var retained: ByteBuffer? = null

    @JniCall
    actual fun storeBorrowed(buffer: ByteBuffer) {
        borrowed = buffer
    }

    @JniCall
    actual fun isStoredInvalidated(): Boolean {
        val buffer = borrowed ?: error("storeBorrowed was not called")
        borrowed = null
        return try {
            buffer.write(byteArrayOf(1))
            false
        } catch (_: BufferReleasedException) {
            buffer.isReleased
        }
    }

    @JniCall
    actual fun storeRetained(buffer: ByteBuffer) {
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
}
