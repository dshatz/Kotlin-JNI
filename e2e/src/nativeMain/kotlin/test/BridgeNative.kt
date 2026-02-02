package test

import dev.datlag.nkommons.JNIConnect
import kotlin.random.Random

@JNIConnect(
    packageName = "kni.test",
    className = "Bridge",
)
fun uppercase(lower: String): String {
    return lower.uppercase()
}

@JNIConnect(
    packageName = "kni.test",
    className = "Bridge",
)
fun byteArray(size: Int): ByteArray {
    return Random.nextBytes(size)
}

@JNIConnect(
    packageName = "kni.test",
    className = "Bridge",
)
fun mixed(number: Long, value: CharArray, upper: Boolean): ByteArray {
    return "$number${value.concatToString()}".let {
        if (upper) it.uppercase()
        else it
    }.encodeToByteArray()
}