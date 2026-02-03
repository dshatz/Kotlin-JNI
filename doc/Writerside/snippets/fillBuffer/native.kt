// Native
import dev.datlag.nkommons.ByteBuffer

@JNIConnect(
    packageName = "org.example",
    className = "MainKt"
)
fun fillBuffer(buffer: ByteBuffer): Boolean {
    val bufferAddress: CPointer<ByteVar> = buffer.address
    val bufferCapacity: Long = buffer.size

    Random.nextBytes(100).usePinned { randomBytes ->
        memcpy(buffer.address, randomBytes.addressOf(0), size.toULong())
    }
    return true
}