package com.dshatz.kni.sample

import com.dshatz.kni.annotations.CallableFromNative
import com.dshatz.kni.buffers.ByteBuffer

@CallableFromNative
interface JvmService: AutoCloseable {

    fun sum(a: Int, b: Int): Int

    fun concat(a: String, b: String): String

    fun printHello()

    fun readBytes(): ByteArray

    fun readBytesTo(buffer: ByteBuffer): Int
}