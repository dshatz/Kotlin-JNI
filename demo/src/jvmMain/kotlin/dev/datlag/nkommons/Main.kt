package dev.datlag.nkommons

import java.io.File
import java.nio.ByteBuffer
import kotlin.random.Random

fun main() {
    System.load(File("build/bin/linuxX64/debugShared/libdemo.so").absolutePath.also { println(it) })

    println("Hello World!")
    println(stringExample())
    println(mixed("One", 2, false, intArrayOf(4, 5), '6'))

    val buffer = ByteBuffer.allocateDirect(4)
    val written = byteBuffer(buffer, 4)
    val contents = ByteArray(4).also {
        buffer.get(it, 0, 4)
    }
    println("Expected: ${written.toHexString()}, Actual: ${contents.toHexString()}")

    println("Sum: ${callJvmFromNative(JvmServiceImpl())}")

    val buffer2 = ByteBuffer.allocateDirect(100)
    val size = writeToJvmBuffer(JvmServiceImpl(), CommonByteBuffer(buffer2))
    val data = ByteArray(size).also {
        buffer2.get(it, 0, size)
    }
    println("Read from buffer on jvm: ${data.toHexString()}")
}

external fun stringExample(): String
external fun mixed(a: String, b: Int, c: Boolean, d: IntArray, e: Char): String
external fun byteBuffer(buffer: ByteBuffer, size: Long): ByteArray
external fun callJvmFromNative(bridge: JvmService): String

external fun writeToJvmBuffer(bridge: JvmService, buffer: CommonByteBuffer): Int

class JvmServiceImpl: JvmService {
    override fun sum(a: Int, b: Int): Int {
        return a + b
    }

    override fun concat(a: String, b: String): String {
        return "$a, $b"
    }

    override fun printHello() {
        println("Hello from JVM!")
    }

    override fun readBytes(): ByteArray {
        return Random.nextBytes(4)
    }

    override fun readBytesTo(buffer: CommonByteBuffer): Int {
        val random = Random.nextBytes(4)
        buffer.buffer.put(random)
        println("Writing to buffer (call from native): ${random.toHexString()}")
        return random.size
    }
}