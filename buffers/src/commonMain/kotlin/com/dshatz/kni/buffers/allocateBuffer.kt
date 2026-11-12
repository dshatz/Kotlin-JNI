package com.dshatz.kni.buffers

expect fun allocateBuffer(size: Long): ByteBuffer

/**
 * Allocate a buffer like [allocateBuffer]. If [wipeOnRelease] is true, [ByteBuffer.release] overwrites it with zeros
 * before releasing it, so sensitive data does not stay in memory.
 *
 * On JVM, memory is only freed by the garbage collector: the wipe happens on [ByteBuffer.release], so a buffer that is
 * never released is never wiped.
 */
fun allocateBuffer(size: Long, wipeOnRelease: Boolean): ByteBuffer {
    return allocateBuffer(size).also { it.wipeOnRelease = wipeOnRelease }
}

internal fun checkCapacityArg(capacity: Long, range: LongRange) {
    if (capacity !in range) error("Capacity must be in range 0..${Int.MAX_VALUE}")
}