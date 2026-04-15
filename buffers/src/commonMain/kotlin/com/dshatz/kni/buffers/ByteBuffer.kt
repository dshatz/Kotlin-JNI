package com.dshatz.kni.buffers

expect class ByteBuffer: IByteBuffer {
    override val capacity: Long
    override fun putInternal(src: ByteArray, dstOffset: Int, length: Int)
    override fun readInternal(dst: ByteArray, offset: Int, length: Int)
    override fun release()
    override val isReleased: Boolean
}


class BufferReleasedException(): Exception("This ByteBuffer had been released.")