package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.annotations.JniCallback
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
     * Store [buffer] without retaining it, from a suspend function.
     */
    @JniCall suspend fun storeBorrowedSuspend(buffer: ByteBuffer)

    /**
     * @return true if the buffer stored by [storeBorrowed] or [storeBorrowedSuspend] has been invalidated.
     */
    @JniCall fun isStoredInvalidated(): Boolean

    /**
     * Retain and store [buffer].
     */
    @JniCall fun storeRetained(buffer: ByteBuffer)

    /**
     * Retain and store [buffer], from a suspend function.
     */
    @JniCall suspend fun storeRetainedSuspend(buffer: ByteBuffer)

    /**
     * Write [value] to the start of the buffer stored by [storeRetained] or [storeRetainedSuspend].
     */
    @JniCall fun writeToRetained(value: Byte)

    /**
     * Release the buffer stored by [storeRetained].
     */
    @JniCall fun releaseRetained()

    @JniCall fun nullableBuffer(buffer: ByteBuffer?): Long

    @JniCall suspend fun nullableBufferSuspend(buffer: ByteBuffer?): Long

    /**
     * Fill [buffer] with [value] after waiting [delayMillis], which is after the JNI call has returned.
     */
    @JniCall suspend fun fillAfterDelay(buffer: ByteBuffer, value: Byte, delayMillis: Long)

    /**
     * @return the first byte of the buffer returned by [BufferProvider.provide].
     */
    @JniCall fun readFromProvider(provider: BufferProvider): Byte

    /**
     * @return the first byte of the buffer returned by [BufferProvider.provideSuspend].
     */
    @JniCall suspend fun readFromSuspendProvider(provider: BufferProvider): Byte
}

/**
 * Returns buffers from JVM to native.
 */
@JniCallback
interface BufferProvider : AutoCloseable {
    fun provide(): ByteBuffer
    suspend fun provideSuspend(): ByteBuffer
}

/**
 * Receives a [ByteBuffer] in its constructor, retaining it if [retain] is true.
 */
expect class BufferHolder(buffer: ByteBuffer, retain: Boolean) : AutoCloseable {
    @JniCall fun isBufferValid(): Boolean

    /**
     * Write [value] to the start of the buffer.
     */
    @JniCall fun writeFirst(value: Byte)

    override fun close()
}
