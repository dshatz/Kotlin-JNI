package com.dshatz.kni.buffers

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteBuffer.toNSData(): NSData {
    return NSData.create(
        bytes = this.address,
        length = this.capacity.toULong()
    )
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteBuffer(): ByteBuffer {
    return ByteBuffer(
        _address =  this.bytes?.reinterpret(),
        capacity = this.length.convert(),
        memoryOwner = this
    )
}