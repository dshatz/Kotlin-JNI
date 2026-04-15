package com.dshatz.kni.buffers

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.nativeHeap

@OptIn(ExperimentalForeignApi::class)
actual fun allocateBuffer(size: Long): ByteBuffer {
    checkCapacityArg(size, 0..Long.MAX_VALUE)
    val ptr = nativeHeap.allocArray<ByteVar>(size)
    return ByteBuffer(
        _address = ptr,
        capacity = size,
        finalizer = {
            nativeHeap.free(ptr.rawValue)
        }
    )
}