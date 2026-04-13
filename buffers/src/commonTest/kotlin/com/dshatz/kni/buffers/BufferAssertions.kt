package com.dshatz.kni.buffers

expect class BufferAssertions(byteBuffer: ByteBuffer) {
    infix fun shouldBeReleased(released: Boolean)
}
