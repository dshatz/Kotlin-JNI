package com.dshatz.kni.buffers

actual fun allocateBuffer(size: Long): ByteBuffer {
    checkCapacityArg(size, 0..Int.MAX_VALUE.toLong()) // allocateDirect takes int, so it's the max size.
    return ByteBuffer(java.nio.ByteBuffer.allocateDirect(size.toInt()))
}