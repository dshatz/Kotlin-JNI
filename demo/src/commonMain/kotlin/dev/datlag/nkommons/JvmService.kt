package dev.datlag.nkommons

import dev.datlag.nkommons.binding.ByteBuffer

@CallableFromNative
interface JvmService: AutoCloseable {

    fun sum(a: Int, b: Int): Int

    fun concat(a: String, b: String): String

    fun printHello()

    fun readBytes(): ByteArray

    fun readBytesTo(buffer: ByteBuffer): Int
}