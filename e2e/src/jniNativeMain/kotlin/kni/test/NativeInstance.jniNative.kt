package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.flows.NativeBackedFlow
import platform.posix.calloc

actual class NativeInstance actual constructor(
    val input: Long,
    private val closeListener: CloseListener
): AutoCloseable, MyInterface {
    init {
        println("[native] Initialized NativeInstance($input)")
    }

    constructor(name: String): this(0, object: CloseListener {
        // This should not be used by the generator. If it is, memory corruption will happen
        // as constructor order can change and wrong types get passed. Tests will fail if it happens.
        override fun onClose() {

        }

        override fun close() {

        }
    })
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

actual class NativeInstance1 actual constructor(private val input: Int): AutoCloseable {
    @JniCall
    actual fun getInput(): Int {
        return input
    }

    actual override fun close() {
    }

}

actual open class NativeCallback: AutoCloseable {
    @JniCall
    actual open  fun receiveFromJvm(value: Int) {
        println("InstanceWithCallback received value from JVM: $value")
    }

    actual override fun close() {
    }
}

actual class IntAccumulator: AutoCloseable {
    private var requestor: RequestIntCallback? = null
    @JniCall
    actual fun init(requestor: RequestIntCallback) {
        this.requestor = requestor
    }

    @JniCall
    actual fun fetchFromJvm() {
        val nativeCallback = object: NativeCallback() {
            override fun receiveFromJvm(value: Int) {
                super.receiveFromJvm(value)
                currentValue.value += value
            }
        }
        println("Fetching int from jvm")
        requestor?.requestInt(nativeCallback)
    }

    actual override fun close() {
        requestor?.close()
    }

    actual val currentValue: NativeBackedFlow<Int> = NativeBackedFlow(0)
}