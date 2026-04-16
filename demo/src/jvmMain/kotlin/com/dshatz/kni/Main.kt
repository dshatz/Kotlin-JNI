package com.dshatz.kni

import com.dshatz.kni.buffers.allocateBuffer
import com.dshatz.kni.load.BundledLibLoader
import com.dshatz.kni.sample.Demo
import com.dshatz.kni.sample.JvmService
import java.io.File
import kotlin.random.Random

fun main() {
    BundledLibLoader.loadBundledLibrary("demo")

    println("Hello World!")
    println(Demo.stringExample())
    println(Demo.mixed("One", 2, false, intArrayOf(4, 5), '6'))

    val buffer = allocateBuffer(4)
    val written = Demo.byteBuffer(buffer, 4)
    val contents = ByteArray(4).also {
        buffer.read(it, 0, 4)
    }
    println("Expected: ${written.toHexString()}, Actual: ${contents.toHexString()}")

    println("Sum: ${Demo.callJvmFromNative(JvmServiceImpl())}")

    val buffer2 = allocateBuffer(100)
    val size = Demo.writeToJvmBuffer(JvmServiceImpl(), buffer2)
    val data = ByteArray(size).also {
        buffer2.read(it, 0, size)
    }
    println("Read from buffer on jvm: ${data.toHexString()}")
}

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

    override fun readBytesTo(buffer: com.dshatz.kni.buffers.ByteBuffer): Int {
        val random = Random.nextBytes(4)
        buffer.put(random)
        println("Writing to buffer (call from native): ${random.toHexString()}")
        return random.size
    }

    override fun close() {
        // Clean up things
    }
}