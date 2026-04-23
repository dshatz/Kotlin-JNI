package com.dshatz.kni.sample

import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.buffers.ByteBuffer

@JniCallback
interface JvmService: AutoCloseable {

    fun sum(a: Int, b: Int): Int

    fun concat(a: String, b: String): String

    fun printHello()

    fun readBytes(): ByteArray

    fun readBytesTo(buffer: ByteBuffer): Int
}