package dev.datlag.nkommons

@CallableFromNative
interface JvmService: Disposable {

    fun sum(a: Int, b: Int): Int

    fun concat(a: String, b: String): String

    fun printHello()

    fun readBytes(): ByteArray

    fun readBytesTo(buffer: ByteBuffer): Int
}