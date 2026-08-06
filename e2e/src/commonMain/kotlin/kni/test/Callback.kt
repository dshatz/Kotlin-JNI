package kni.test

import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.buffers.allocateBuffer
import kni.test.serializable.ColorfulObject

@JniCallback
interface Callback: AutoCloseable {
    fun onComplete(result: Boolean): Result<String>
    fun onCompleteWithData(data: ColorfulObject): Result<ByteArray>?

    fun onCompleteWithNullable(
        str: String? = "",
        i: Int? = 1,
        f: Float? = 1f,
        d: Double? = 1.0,
        c: Char? = 'c',
        short: Short? = 1,
        byte: Byte? = 1,
        byteArray: ByteArray? = byteArrayOf(1),
        charArray: CharArray? = charArrayOf('c'),
        intArray: IntArray? = intArrayOf(1),
        floatArray: FloatArray? = floatArrayOf(1f),
        doubleArray: DoubleArray? = doubleArrayOf(1.0),
        serializable: ColorfulObject? = ColorfulObject("orange", 2.0, 1..2),
        buffer: ByteBuffer? = allocateBuffer(1024 * 1025),
    )

    fun onCompleteWithGeneric(result: Result<Int>)
}

@JniCallback
interface ThrowingCallback: AutoCloseable {
    fun unstable(shouldThrow: Boolean)

    fun nullable(shouldThrow: Boolean, returnNull: Boolean): String?
}

/**
 * Example callback that native can call to request some data.
 *
 * [requestInt] can run asynchronously on JVM, calling [NativeCallback] when it is done.
 */
@JniCallback
interface RequestIntCallback: AutoCloseable {
    fun requestInt(nativeCallback: NativeCallback)
}