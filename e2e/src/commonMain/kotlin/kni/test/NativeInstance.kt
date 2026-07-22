package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.buffers.ByteBuffer

// Normally using a listener for close does not make sense as close is called from JVM anyway.
// We pass it to check in tests.
expect class NativeInstance(
    input: Long,
    closeListener: CloseListener
): AutoCloseable, MyInterface {
    @JniCall
    override fun negate(): Long

    @JniCall
    fun fillBuffer(buffer: ByteBuffer, callback: BufferFillCallback)
    override fun close()
}

interface MyInterface {
    fun negate(): Long
}

@JniCallback
interface BufferFillCallback: AutoCloseable {
    fun onFilled(buffer: ByteBuffer, hex: String)
}

@JniCallback
interface CloseListener: AutoCloseable {
    fun onClose()
}