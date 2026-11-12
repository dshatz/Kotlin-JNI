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

    actual override fun wipeInternal() {
        if (jvmBuffer.hasArray()) {
            val start = jvmBuffer.arrayOffset()
            java.util.Arrays.fill(jvmBuffer.array(), start, start + jvmBuffer.capacity(), 0)
        } else {
            val view = jvmBuffer.duplicate()
            view.clear()
            while (view.remaining() >= Long.SIZE_BYTES) view.putLong(0L)
            while (view.hasRemaining()) view.put(0)
        }
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
        if (_released) return
        if (wipeOnRelease) wipeInternal()
        _released = true
        // no-op, to release a jvm buffer the ByteBuffer needs to be garbage collected on jvm.
    }

    private var _released = false
    actual override val isReleased: Boolean get() = _released
}

fun ByteBuffer.toKByteBuffer(): com.dshatz.kni.buffers.ByteBuffer {
    return ByteBuffer(this)
}