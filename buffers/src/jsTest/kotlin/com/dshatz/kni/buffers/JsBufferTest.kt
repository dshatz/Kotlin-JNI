package com.dshatz.kni.buffers

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import kotlin.random.Random

@OptIn(ExperimentalUnsignedTypes::class)
val JsBufferTest by testSuite("JsBufferTest") {
    test("allocate and write") {
        val buffer = allocateBuffer(100)

        val bytes = Random.nextBytes(100)
        buffer.put(bytes)
        buffer.readToByteArray(0, 100).toHexString() shouldBe bytes.toHexString()
    }

    test("to blob") {
        val buffer = allocateBuffer(100)
        val blob = buffer.toBlob()

        val bytes = Random.nextBytes(100)
        buffer.put(bytes)

        // Blob is a snapshot, it will not reflect changes to original buffer.
        blob.size shouldBe 100
        blob.toByteArray().toHexString() shouldBe "00".repeat(100)

        // Create a blob again, now it is updated.
        val newBlob = buffer.toBlob()
        newBlob.toByteArray().toHexString() shouldBe bytes.toHexString()
    }

    test("as int8array") {
        val buffer = allocateBuffer(100)
        val array = buffer.asInt8Array()
        val bytes = Random.nextBytes(100)
        buffer.put(bytes)

        array.buffer.byteLength shouldBe 100
        (array.asDynamic() as ByteArray).toHexString() shouldBe bytes.toHexString()
    }
}