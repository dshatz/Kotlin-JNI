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
}