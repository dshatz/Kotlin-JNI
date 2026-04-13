package com.dshatz.kni.buffers

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.cinterop.ExperimentalForeignApi

actual class BufferAssertions actual constructor(private val byteBuffer: ByteBuffer) {
    @OptIn(ExperimentalForeignApi::class)
    actual infix fun shouldBeReleased(released: Boolean) {
        withClue("Expected ByteBuffer to ${"not ".takeUnless { released }.orEmpty()}be released") {
            if (released) {
                byteBuffer._address shouldBe null
            } else {
                byteBuffer._address shouldNotBe null
            }
        }
    }
}