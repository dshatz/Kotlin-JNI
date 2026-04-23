import com.dshatz.kni.buffers.ByteBuffer

// commonMain
@JniCall
expect fun fillBuffer(buffer: ByteBuffer): String

// Native
actual fun fillBuffer(buffer: ByteBuffer): String {
    val bytes = Random.nextBytes(buffer.capacity.toInt())
    buffer.put(bytes)
    return bytes.toHexString()
}