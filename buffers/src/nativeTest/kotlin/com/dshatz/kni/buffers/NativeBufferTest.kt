package com.dshatz.kni.buffers

import de.infix.testBalloon.framework.core.testSuite
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
val NativeBufferTest by testSuite {
    test("release native heap") {
        val buffer = allocateBuffer(100)

        // Owns the memory on native heap.
        buffer.memoryOwner shouldBe null

        buffer.release()
        buffer._address shouldBe null
    }

    test("wrap ByteArray") {
        val bytes = Random.nextBytes(100)
        val buffer = ByteBuffer.wrapArray(bytes)

        buffer.memoryOwner should beInstanceOf<Pinned<ByteArray>>()
        (buffer.memoryOwner as Pinned<ByteArray>).get() shouldBe bytes
        buffer.capacity shouldBe bytes.size

        buffer.readToByteArray(0, 100).toHexString() shouldBe bytes.toHexString()

        val pinned: Pinned<ByteArray> = buffer.memoryOwner as Pinned<ByteArray>
        buffer.release()
        buffer._address shouldBe null
        buffer.memoryOwner shouldBe null

        // release should unpin the memory owner.
        shouldThrow<NullPointerException> {
            pinned.get()
        }
    }

    test("wrap address") {
        // Wrap a raw address.
        val (b, arr) = memScoped {
            val arr = allocArray<ByteVar>(1024)
            val buffer = ByteBuffer.wrapAddress(arr, 1024)
            buffer.address shouldBe arr
            buffer.memoryOwner shouldNot beNull()
            buffer.capacity shouldBe 1024

            arr[0] = 0x99.toByte()
            arr[1023] = 0xFF.toByte()

            buffer.readToByteArray(0, 1).first() shouldBe 0x99.toByte()
            buffer.readToByteArray(1023, 1).first() shouldBe 0xFF.toByte()
            buffer to arr
        }

        // Buffer is invalid outside of this block!
        val firstByte = b.readToByteArray(0, 1).first()
        firstByte shouldNotBe 0x99.toByte()
        firstByte shouldBe arr[0] // Still points to the same array, but it's now trash.

        b.release()
        b.memoryOwner shouldBe null
        b._address shouldBe null
    }
}