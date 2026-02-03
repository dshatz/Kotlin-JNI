// JVM / Android
external fun fillBuffer(buffer: java.nio.ByteBuffer): Boolean

fun main() {
    val buffer = ByteBuffer.allocateDirect(100)
    fillBuffer(buffer)
}