package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.flows.NativeBackedFlow

// Normally using a listener for close does not make sense as close is called from JVM anyway.
// We pass it to check in tests.
expect class NativeInstance(
    closeListener: CloseListener,
    input: Long
): AutoCloseable, MyInterface {

    constructor(
        input: Long,
        closeListener: CloseListener
    )
    @JniCall
    override fun negate(): Long

    @JniCall
    fun fillBuffer(buffer: ByteBuffer, callback: BufferFillCallback)
    override fun close()
}

expect class NativeInstance1(
    input: Int
): AutoCloseable {
    @JniCall fun getInput(): Int
    override fun close()
}

interface MyInterface {
    fun negate(): Long
}

/**
 * An example NativeInstance that is essentially a callback from jvm to native.
 *
 * Use: Create an instance of this on Native side and pass it to JVM to receive asynchronous events.
 */
expect class NativeCallback(): AutoCloseable {
    @JniCall
    fun receiveFromJvm(value: Int)
    override fun close()
}

expect class IntAccumulator(): AutoCloseable {
    @JniCall
    fun init(requestor: RequestIntCallback)

    @JniCall
    fun fetchFromJvm()

    val currentValue: NativeBackedFlow<Int>

    override fun close()
}

@JniCallback
interface BufferFillCallback: AutoCloseable {
    fun onFilled(buffer: ByteBuffer, hex: String)
}

@JniCallback
interface CloseListener: AutoCloseable {
    fun onClose()
}