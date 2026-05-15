package kni.test

import com.dshatz.kni.annotations.JniCall
import kotlin.random.Random

actual object Bridge {
    @JniCall
    actual fun uppercase(lower: String): String {
        return lower.uppercase()
    }

    @JniCall
    actual fun byteArray(length: Int): ByteArray {
        return Random.nextBytes(length)
    }

    @JniCall
    actual fun mixed(number: Long, value: CharArray, upper: Boolean, char: Char): String {
        return "$number$char${value.concatToString()}".let {
            if (upper) it.uppercase()
            else it
        }
    }

    @JniCall
    actual fun withTypeAlias(long: TestAlias): TestAlias {
        return long
    }

    @JniCall
    actual fun serializable(obj: ColorfulObject): String {
        return obj.color
    }
}