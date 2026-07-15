package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.buffers.allocateBuffer

expect object CommonCallable {

    @JniCall
    fun makeOrange(value: ColorfulObject): ColorfulObject

    @JniCall
    fun fillBuffer(buffer: ByteBuffer): String

    @JniCall
    fun longArray(value: LongArray): LongArray

    @JniCall
    fun byteArray(value: ByteArray): ByteArray

    @JniCall
    fun shortArray(value: ShortArray): ShortArray

    @JniCall
    fun booleanArray(value: BooleanArray): BooleanArray

    @JniCall
    fun charArray(value: CharArray): CharArray

    @JniCall
    fun doubleArray(value: DoubleArray): DoubleArray

    @JniCall
    fun floatArray(value: FloatArray): FloatArray

    @JniCall
    fun intArray(value: IntArray): IntArray

    @JniCall
    fun anyIsNull(
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
        callback: Callback? = NoopCallback()
    ): Boolean

}

class NoopCallback: Callback {
    override fun onComplete(result: Boolean) {

    }

    override fun onCompleteWithData(data: ColorfulObject) {

    }

    override fun onCompleteWithNullable(
        str: String?,
        i: Int?,
        f: Float?,
        d: Double?,
        c: Char?,
        short: Short?,
        byte: Byte?,
        byteArray: ByteArray?,
        charArray: CharArray?,
        intArray: IntArray?,
        floatArray: FloatArray?,
        doubleArray: DoubleArray?,
        serializable: ColorfulObject?,
        buffer: ByteBuffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun close() {

    }
}

@JniCall
expect fun topLevelFun(): ByteBuffer
