package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.buffers.allocateBuffer
import kni.test.Bridge.byteArray
import kotlin.random.Random

actual object CommonCallable {
    @JniCall
    actual fun makeOrange(value: ColorfulObject): ColorfulObject {
        return value.copy(color = "orange")
    }

    @JniCall
    actual fun fillBuffer(buffer: ByteBuffer): String {
        val bytes = Random.nextBytes(buffer.capacity.toInt())
        buffer.write(bytes)
        return bytes.toHexString()
    }

    @JniCall
    actual fun anyIsNull(
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
        buffer: ByteBuffer?,
        callback: Callback?
    ): Boolean {
        println("Int: $i")
        return listOf(
            str,
            i,
            f,
            d,
            c,
            short,
            byte,
            byteArray,
            charArray,
            intArray,
            floatArray,
            doubleArray,
            serializable,
            buffer,
            callback
        ).any { it == null }
    }
}

@JniCall
actual fun topLevelFun(): ByteBuffer {
    val buffer = allocateBuffer(1024)
    return buffer
}