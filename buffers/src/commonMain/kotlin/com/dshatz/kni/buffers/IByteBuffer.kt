package com.dshatz.kni.buffers

abstract class IByteBuffer internal constructor() {
    abstract val capacity: Long

    /**
     * Write [length] bytes from [src] to this buffer starting at [dstOffset].
     *
     * @throws BufferReleasedException if buffer had been released using [release].
     * @throws IllegalArgumentException in case of data size inconsistencies.
     */
    fun write(src: ByteArray, dstOffset: Int = 0, length: Int = src.size) {
        if (isReleased) throw BufferReleasedException()
        if (dstOffset < 0) throw IllegalArgumentException("Offset cannot be negative, got: $dstOffset")
        if (dstOffset + length > capacity) throw IllegalArgumentException("Buffer overflow: offset ($dstOffset) + length ($length) > capacity ($capacity)")
        if (length > src.size) throw IllegalArgumentException("Source underflow: attempted reading $length, src size: ${src.size}")
        writeInternal(src, dstOffset, length)
    }

    protected abstract fun writeInternal(src: ByteArray, dstOffset: Int = 0, length: Int = src.size)

    /**
     * Read [length] bytes from this buffer starting at [offset] to [dst].
     *
     * @throws BufferReleasedException if buffer had been released using [release].
     * @throws IllegalArgumentException in case of data size inconsistencies.
     */
    fun read(dst: ByteArray, offset: Int, length: Int) {
        if (isReleased) throw BufferReleasedException()
        if (offset + length > capacity) throw IllegalArgumentException("Buffer underflow: offset ($offset) + length ($length) > capacity $capacity")
        if (length > dst.size) throw IllegalArgumentException("Destination overflow: attempted to read ${length}, dst capacity: ${dst.size}")
        readInternal(dst, offset, length)
    }

    protected abstract fun readInternal(dst: ByteArray, offset: Int, length: Int)

    /**
     * Release underlying platform-dependant buffer.
     *
     * Calling [read] or [write] after [release] will throw [BufferReleasedException].
     */
    abstract fun release()

    abstract val isReleased: Boolean

    /**
     * Read [length] bytes from offset [offset] of this buffer to a new [ByteArray].
     */
    fun readToByteArray(offset: Int, length: Int): ByteArray {
        val dst = ByteArray(length)
        read(dst, offset, length)
        return dst
    }
}