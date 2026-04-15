package com.dshatz.kni.buffers

expect fun allocateBuffer(size: Long): ByteBuffer

internal fun checkCapacityArg(capacity: Long, range: LongRange) {
    if (capacity !in range) error("Capacity must be in range 0..${Int.MAX_VALUE}")
}