package com.dshatz.kni.buffers

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import kotlin.random.Random

val JvmBufferTest by testSuite {

    test("convert") {
        val jvmBuffer = java.nio.ByteBuffer.allocateDirect(1024)
        val buffer = jvmBuffer.toCommonByteBuffer()

        jvmBuffer shouldBe buffer.toNioByteBuffer()
        buffer.capacity shouldBe jvmBuffer.capacity()
        buffer.jvmBuffer.isDirect shouldBe true
    }

    test("write read") {
        val jvmBuffer = java.nio.ByteBuffer.allocateDirect(1024)
        val buffer = jvmBuffer.toCommonByteBuffer()
        val bytes = Random.nextBytes(jvmBuffer.capacity())

        buffer.write(bytes)

        val read = ByteArray(1024)
        jvmBuffer.get(read, 0, 1024)

        read.toHexString() shouldBe bytes.toHexString()
    }

    test("wipe on release") {
        val buffer = allocateBuffer(64, wipeOnRelease = true)
        buffer.write(Random.nextBytes(64))
        val jvmBuffer = buffer.jvmBuffer
        buffer.release()
        (0 until 64).all { jvmBuffer.get(it) == 0.toByte() } shouldBe true
    }

    test("no wipe on release by default") {
        val buffer = allocateBuffer(1)
        buffer.write(byteArrayOf(7))
        buffer.release()
        buffer.jvmBuffer.get(0) shouldBe 7
    }

    test("wipe heap buffer") {
        val array = Random.nextBytes(16)
        java.nio.ByteBuffer.wrap(array).toCommonByteBuffer().wipe()
        array.all { it == 0.toByte() } shouldBe true
    }
}
