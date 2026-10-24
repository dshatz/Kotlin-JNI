package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.buffers.ByteBuffer

/**
 * Exercises the lifetime of [ByteBuffer] parameters, which are borrowed for the duration of a blocking call.
 */
expect object BufferKeeper {
    /**
     * Store [buffer] without retaining it.
     */
    @JniCall fun storeBorrowed(buffer: ByteBuffer)

    /**
     * @return true if the buffer stored by [storeBorrowed] was invalidated when that call returned.
     */
    @JniCall fun isStoredInvalidated(): Boolean

    /**
     * Retain and store [buffer].
     */
    @JniCall fun storeRetained(buffer: ByteBuffer)

    /**
     * Write [value] to the start of the buffer stored by [storeRetained].
     */
    @JniCall fun writeToRetained(value: Byte)

    /**
     * Release the buffer stored by [storeRetained].
     */
    @JniCall fun releaseRetained()

    @JniCall fun nullableBuffer(buffer: ByteBuffer?): Long
}
