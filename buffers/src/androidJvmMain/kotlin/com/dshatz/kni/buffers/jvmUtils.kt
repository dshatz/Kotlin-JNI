package com.dshatz.kni.buffers

import com.dshatz.kni.annotations.KniInternalApi

fun java.nio.ByteBuffer.toCommonByteBuffer(): ByteBuffer {
    return ByteBuffer(this)
}

fun ByteBuffer.toNioByteBuffer(): java.nio.ByteBuffer {
    return this.jvmBuffer
}

/**
 * The underlying java.nio.ByteBuffer, for passing to native code.
 *
 * @throws IllegalArgumentException if it is not a direct buffer: native code can only access direct buffers.
 */
@KniInternalApi
fun ByteBuffer.toDirectNioByteBuffer(): java.nio.ByteBuffer {
    require(jvmBuffer.isDirect) { "Only direct buffers can be passed to native code. Use allocateBuffer or java.nio.ByteBuffer.allocateDirect." }
    return jvmBuffer
}
