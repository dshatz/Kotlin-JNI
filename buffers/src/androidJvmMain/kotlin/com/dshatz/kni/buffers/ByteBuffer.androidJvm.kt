package com.dshatz.kni.buffers

import java.nio.ByteBuffer


actual data class ByteBuffer(val jvmBuffer: ByteBuffer) : IByteBuffer() {
    actual override val capacity: Long get() = jvmBuffer.capacity().toLong()

    actual override fun writeInternal(src: ByteArray, dstOffset: Int, length: Int) {
        val view = jvmBuffer.duplicate()
        view.position(dstOffset)
        view.put(src, 0, length)
    }

    actual override fun readInternal(dst: ByteArray, offset: Int, length: Int) {
        val view = jvmBuffer.duplicate()
        view.position(offset)
        view.get(dst, 0, length)
    }

    /**
     * Mark this buffer as released.
     *
     * In reality, the underlying [java.nio.ByteBuffer] will only get released after this object is garbage-collected.
     * Caller should stop using this object and remove all references to it so the memory actually gets freed.

     * Calling [read] or [write] after [release] will throw [BufferReleasedException].
     *
     * @see IByteBuffer.release
     */
    actual override fun release() {
        _released = true
        // no-op, to release a jvm buffer the ByteBuffer needs to be garbage collected on jvm.
    }

    private var _released = false
    actual override val isReleased: Boolean get() = _released
}