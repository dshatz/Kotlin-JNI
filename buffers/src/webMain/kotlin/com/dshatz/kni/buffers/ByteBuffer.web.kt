package com.dshatz.kni.buffers

actual class ByteBuffer(
    internal var storage: ByteArray?,
    actual override val capacity: Long
) : IByteBuffer() {

    val byteArray: ByteArray get() = storage ?: throw BufferReleasedException()

    /**
     * Mark this ByteBuffer as released.
     *
     * The underlying [ByteArray] storage will get garbage-collected.
     *
     * Calling [read] or [put] after [release] will throw [BufferReleasedException].
     *
     * @see [IByteBuffer.release]
     */
    actual override fun release() {
        _released = true
        storage = null
    }

    private var _released = false
    actual override val isReleased: Boolean
        get() = _released

    actual override fun putInternal(src: ByteArray, dstOffset: Int, length: Int) {
        src.copyInto(
            destination = byteArray,
            destinationOffset = dstOffset,
            endIndex = length
        )
    }

    actual override fun readInternal(dst: ByteArray, offset: Int, length: Int) {
        byteArray.copyInto(
            destination = dst,
            startIndex = offset,
            endIndex = offset + length
        )
    }
}