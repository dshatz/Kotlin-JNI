package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.buffers.allocateBuffer
import kni.test.serializable.ColorfulObject
import kni.test.serializable.PolymorphicFruit
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
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

    @OptIn(ExperimentalForeignApi::class)
    @JniCall
    actual fun callThrowingCallback(callback: ThrowingCallback, shouldThrow: Boolean) {
        val result = runCatching {
            callback.unstable(shouldThrow)
        }.onFailure {
            println("Native received exception! ${it}")
            lastError = it.message
        }.onSuccess {
            println("Native received no exception from callback")
            lastError = null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    @JniCall
    actual fun callThrowingNullableCallback(callback: ThrowingCallback, shouldThrow: Boolean, returnNull: Boolean): String? {
        return runCatching {
            callback.nullable(shouldThrow, returnNull)
        }.onFailure {
            println("Native received exception! ${it}")
            lastError = it.message
        }.onSuccess {
            println("Native received no exception from callback")
            lastError = null
        }.getOrNull()
    }

    private var lastError: String? = null

    @JniCall
    actual fun getError(): String? {
        return lastError
    }

    @JniCall
    actual suspend fun doWork(fail: Boolean, orange: PolymorphicFruit): ByteBuffer {
        if (fail) error("Simulated error")
        return allocateBuffer(1)
    }

    @JniCall
    actual fun manyObjects(objs: Array<Callback>): Int {
        return objs.size
    }
}

@JniCall
actual fun topLevelFun(): ByteBuffer {
    val buffer = allocateBuffer(1024)
    return buffer
}
