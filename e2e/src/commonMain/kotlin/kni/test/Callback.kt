package kni.test

import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.buffers.allocateBuffer

@JniCallback
interface Callback: AutoCloseable {
    fun onComplete(result: Boolean)
    fun onCompleteWithData(data: ColorfulObject)

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
        buffer: ByteBuffer? = allocateBuffer(1024 * 1024),
    )
}