package com.dshatz.kni.buffers

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

actual class BufferAssertions actual constructor(private val byteBuffer: ByteBuffer) {
    actual infix fun shouldBeReleased(released: Boolean) {
        withClue("Expected ByteBuffer to ${"not ".takeUnless { released }.orEmpty()}be released") {
            if (released) {
                byteBuffer.storage shouldBe null
            } else {
                byteBuffer.storage shouldNotBe null
            }
        }
    }
}