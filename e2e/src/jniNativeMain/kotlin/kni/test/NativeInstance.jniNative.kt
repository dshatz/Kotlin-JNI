package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.buffers.ByteBuffer

actual class NativeInstance actual constructor(
    val input: Long,
    private val closeListener: CloseListener
): AutoCloseable, MyInterface {
    init {
        println("[native] Initialized NativeInstance($input)")
    }
    @JniCall
    actual override fun negate(): Long {
        println("[native] Negating $input")
        return -input
    }

    @JniCall
    actual fun fillBuffer(buffer: ByteBuffer, callback: BufferFillCallback) {
        val arr = ByteArray(buffer.capacity.toInt())
        arr.fill(input.toByte())
        buffer.write(arr)
        callback.onFilled(buffer, arr.toHexString())
    }

    actual override fun close() {
        closeListener.onClose()
        println("[native] close()")
    }

    actual constructor(closeListener: CloseListener, input: Long) : this(input, closeListener)
}

actual class NativeInstance1 actual constructor(private val input: Long): AutoCloseable {
    @JniCall
    actual fun getInput(): Long {
        return input
    }

    actual override fun close() {
    }

}