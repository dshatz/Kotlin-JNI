package kni.test

import com.dshatz.kni.annotations.Callable
import com.dshatz.kni.buffers.ByteBuffer
import kotlin.random.Random

actual object CommonCallable {
    @Callable
    actual fun makeOrange(value: ColorfulObject): ColorfulObject {
        return value.copy(color = "orange")
    }

    @Callable
    actual fun fillBuffer(buffer: ByteBuffer): String {
        val bytes = Random.nextBytes(buffer.capacity.toInt())
        buffer.put(bytes)
        return bytes.toHexString()
    }
}