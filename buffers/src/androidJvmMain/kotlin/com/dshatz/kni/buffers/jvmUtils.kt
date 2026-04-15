package com.dshatz.kni.buffers

fun java.nio.ByteBuffer.toCommonByteBuffer(): ByteBuffer {
    return ByteBuffer(this)
}

fun ByteBuffer.toNioByteBuffer(): java.nio.ByteBuffer {
    return this.jvmBuffer
}