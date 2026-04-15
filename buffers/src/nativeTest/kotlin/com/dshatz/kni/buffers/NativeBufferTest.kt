package com.dshatz.kni.buffers

import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestElementName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import kotlin.random.Random

@OptIn(ExperimentalForeignApi::class)
val NativeBufferTest by testSuite("NativeBufferTest") {
    test("release native heap") {
        val buffer = allocateBuffer(100)

        buffer.release()
        buffer._address shouldBe null
    }

    test("wrap ByteArray") {
        val bytes = Random.nextBytes(100)
        val buffer = ByteBuffer.wrapArray(bytes)

        buffer.capacity shouldBe bytes.size

        buffer.readToByteArray(0, 100).toHexString() shouldBe bytes.toHexString()

        buffer.release()
        buffer._address shouldBe null
    }

    test("wrap address") {
        // Wrap a raw address.
        val (b, arr) = memScoped {
            val arr = allocArray<ByteVar>(1024)
            val buffer = ByteBuffer.wrapAddressMemScope(this, arr, 1024)
            buffer.address shouldBe arr
            buffer.capacity shouldBe 1024

            arr[0] = 0x99.toByte()
            arr[1023] = 0xFF.toByte()

            buffer.readToByteArray(0, 1).first() shouldBe 0x99.toByte()
            buffer.readToByteArray(1023, 1).first() shouldBe 0xFF.toByte()
            buffer to arr
        } // release should be automatically called here.

        // Buffer is invalid outside of this block!
        shouldThrow<BufferReleasedException> {
            b.readToByteArray(0, 1).first()
        }

        b._address shouldBe null
    }
}