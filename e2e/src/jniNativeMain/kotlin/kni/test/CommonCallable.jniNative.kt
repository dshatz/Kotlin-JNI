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
    actual fun longArray(value: LongArray): LongArray {
        return value.map { it * 2 }.toLongArray()
    }

    @JniCall
    actual fun byteArray(value: ByteArray): ByteArray {
        return value.reversedArray()
    }

    @JniCall
    actual fun shortArray(value: ShortArray): ShortArray {
        return value.reversedArray()
    }

    @JniCall
    actual fun booleanArray(value: BooleanArray): BooleanArray {
        return value.reversedArray()
    }

    @JniCall
    actual fun charArray(value: CharArray): CharArray {
        return value.reversedArray()
    }

    @JniCall
    actual fun doubleArray(value: DoubleArray): DoubleArray {
        return value.map { it * 2 }.toDoubleArray()
    }

    @JniCall
    actual fun floatArray(value: FloatArray): FloatArray {
        return value.map { it * 2 }.toFloatArray()
    }

    @JniCall
    actual fun intArray(value: IntArray): IntArray {
        return value.map { it * 2 }.toIntArray()
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
