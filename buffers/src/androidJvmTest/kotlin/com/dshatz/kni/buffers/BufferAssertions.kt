package com.dshatz.kni.buffers

actual class BufferAssertions actual constructor(private val byteBuffer: ByteBuffer) {
    actual infix fun shouldBeReleased(released: Boolean) {
        // noop
    }
}