package com.dshatz.kni.buffers

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.objc_release
import kotlinx.cinterop.objc_retain
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
    val retainedData = this.objcPtr()
    objc_retain(retainedData)
    return ByteBuffer(
        _address =  this.bytes?.reinterpret(),
        capacity = this.length.convert(),
        finalizer = {
            objc_release(retainedData)
        }
    )
}