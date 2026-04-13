package com.dshatz.kni.buffers

actual fun allocateBuffer(size: Long): ByteBuffer {
    checkCapacityArg(size, 0..Int.MAX_VALUE.toLong())
    return ByteBuffer(storage = ByteArray(size.toInt()), capacity = size)
}