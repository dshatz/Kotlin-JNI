package com.dshatz.kni.sample

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.buffers.ByteBuffer

expect object Demo {
    @JniCall fun stringExample(): String
    @JniCall fun mixed(a: String, b: Int, c: Boolean, d: IntArray, e: Char): String
    @JniCall fun byteBuffer(buffer: ByteBuffer, size: Long): ByteArray
    @JniCall fun callJvmFromNative(obj: JvmService): String

    @JniCall fun writeToJvmBuffer(bridge: JvmService, buffer: ByteBuffer): Int
}