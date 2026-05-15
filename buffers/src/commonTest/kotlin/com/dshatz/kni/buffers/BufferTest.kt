package com.dshatz.kni.buffers

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.random.Random

val BufferTest by testSuite() {

    test("write read") {
        val bytes = Random.nextBytes(100)
        val buffer = allocateBuffer(bytes.size.toLong())
        buffer.capacity shouldBe bytes.size
        buffer.write(bytes)

        buffer.readToByteArray(0, 100).toHexString() shouldBe bytes.toHexString()
    }

    test("release") {
        val b = allocateBuffer(1024)
        b.release()
        with(BufferAssertions(b)) {
            this shouldBeReleased true
        }
        shouldThrow<BufferReleasedException> {
            b.read(ByteArray(10), 0, 10)
        }

        shouldThrow<BufferReleasedException> {
            b.write(ByteArray(10), 0, 10)
        }
    }

    test("allocate negative size") {
        shouldThrow<Exception> {
            allocateBuffer(-1L)
        }
    }

    test("read out of bounds") {
        val buffer = allocateBuffer(100)
        shouldThrow<IllegalArgumentException> {
            buffer.read(byteArrayOf(10), 0, 1000)
        }
    }

    test("read overflow destination") {
        val buffer = allocateBuffer(100)
        shouldThrow<IllegalArgumentException> {
            buffer.read(byteArrayOf(10), 0, 100)
        }
    }

    test("read from negative offset") {
        val buffer = allocateBuffer(100)
        shouldThrow<IllegalArgumentException> {
            buffer.read(byteArrayOf(10), -1, 10)
        }
    }

    test("write negative offset") {
        val buffer = allocateBuffer(999999)
        shouldThrow<IllegalArgumentException> {
            buffer.write(Random.nextBytes(buffer.capacity.toInt()), -1)
        }
    }

    test("write past end") {
        val buffer = allocateBuffer(999999)
        shouldThrow<IllegalArgumentException> {
            buffer.write(Random.nextBytes(buffer.capacity.toInt()), 1)
        }
    }
}