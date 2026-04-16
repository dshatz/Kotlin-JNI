package com.dshatz.kni.sample

import com.dshatz.kni.annotations.Callable
import com.dshatz.kni.buffers.ByteBuffer

expect object Demo {
    @Callable fun stringExample(): String
    @Callable fun mixed(a: String, b: Int, c: Boolean, d: IntArray, e: Char): String
    @Callable fun byteBuffer(buffer: ByteBuffer, size: Long): ByteArray
    @Callable fun callJvmFromNative(obj: JvmService): String

    @Callable fun writeToJvmBuffer(bridge: JvmService, buffer: ByteBuffer): Int
}