package test

import dev.datlag.nkommons.JNIClassName
import dev.datlag.nkommons.JNIConnect
import dev.datlag.nkommons.JNIPackageName
import kotlin.random.Random

@JNIConnect
@JNIPackageName("kni.test")
@JNIClassName("Bridge")
fun uppercase(lower: String): String {
    return lower.uppercase()
}

@JNIConnect
@JNIPackageName("kni.test")
@JNIClassName("Bridge")
fun byteArray(size: Int): ByteArray {
    return Random.nextBytes(size)
}

@JNIConnect
@JNIPackageName("kni.test")
@JNIClassName("Bridge")
fun mixed(number: Long, value: CharArray, upper: Boolean): ByteArray {
    return "$number${value.concatToString()}".let {
        if (upper) it.uppercase()
        else it
    }.encodeToByteArray()
}