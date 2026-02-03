import dev.datlag.nkommons.JNIConnect
import dev.datlag.nkommons.*
import dev.datlag.nkommons.utils.memcpy
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlin.random.Random

@JNIConnect
fun boolExample(value: Boolean): Boolean {
    return !value
}

@JNIConnect
fun boolArrayExample(value: BooleanArray): BooleanArray {
    return value.reversedArray()
}

@JNIConnect
fun byteExample(value: Byte): Byte {
    return value
}

@JNIConnect
fun byteArrayExample(value: ByteArray): ByteArray {
    return value.reversedArray()
}

@JNIConnect
fun charExample(value: Char): Char {
    return if (value.isUpperCase()) {
        value.lowercaseChar()
    } else {
        value.uppercaseChar()
    }
}

@JNIConnect
fun charArrayExample(value: CharArray): CharArray {
    return value.reversedArray()
}

@JNIConnect
fun doubleExample(value: Double): Double {
    return value + 0.2
}

@JNIConnect
fun doubleArrayExample(value: DoubleArray): DoubleArray {
    return value.reversedArray()
}

@JNIConnect
fun floatExample(value: Float): Float {
    return value + 0.5F
}

@JNIConnect
fun floatArrayExample(value: FloatArray): FloatArray {
    return value.reversedArray()
}

@JNIConnect
fun intExample(value: Int): Int {
    return value + 2
}

@JNIConnect
fun intArrayExample(value: IntArray): IntArray {
    return value.reversedArray()
}

@JNIConnect
fun longExample(value: Long): Long {
    return value + 8L
}

@JNIConnect
fun longArrayExample(value: LongArray): LongArray {
    return value.reversedArray()
}

@JNIConnect
fun shortExample(value: Short): Short {
    return (value + 2).toShort()
}

@JNIConnect
fun shortArrayExample(value: ShortArray): ShortArray {
    return value.reversedArray()
}

@JNIConnect("dev.datlag.nkommons", "MainKt")
fun stringExample(): String {
    return "Hello Native!"
}

@JNIConnect
fun addition(a: Int, b: Long): Int {
    return a + b.toInt()
}

@JNIConnect
fun concat(a: String, b: String): String {
    return "$a$b"
}

@JNIConnect("dev.datlag.nkommons", "MainKt")
fun mixed(a: String, b: Int, c: Boolean, d: IntArray, e: Char): String {
    return "$a, $b, $c, ${d.joinToString(separator = "|", prefix = "[", postfix = "]")}, $e"
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
@JNIConnect(
    packageName = "dev.datlag.nkommons",
    className = "MainKt"
)
fun byteBuffer(buffer: ByteBuffer, size: Long): ByteArray {
    val bytes = Random.nextBytes(size.toInt())
    bytes.usePinned {
        memcpy(buffer.address, it.addressOf(0), size.toULong())
    }
    return bytes
}

@JNIConnect(
    packageName = "dev.datlag.nkommons",
    className = "MainKt"
)
fun callJvmFromNative(obj: JvmService): String {
    val sum = obj.sum(100, 200).toString()
    val greeting = obj.concat("Hello", "Jni")
    obj.printHello()
    println("Native read bytes from jvm: ${obj.readBytes().toHexString()}")
    return obj.concat(sum, greeting)
}

@JNIConnect(
    packageName = "dev.datlag.nkommons",
    className = "MainKt"
)
fun writeToJvmBuffer(bridge: JvmService, buffer: ByteBuffer): Int {
    return bridge.readBytesTo(buffer)
}

lateinit var jvmCallback: JvmCallback

// Native
@JNIConnect(
    packageName = "com.example",
    className = "MainKt"
)
fun init(callback: JvmCallback) {
    // Save the object for later.
    jvmCallback = callback
}

@JNIConnect(
    packageName = "com.example",
    className = "MainKt"
)
fun dispose() {
    jvmCallback.dispose()
}