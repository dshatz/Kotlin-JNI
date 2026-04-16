package kni.test

import com.dshatz.kni.annotations.Callable
import kotlin.random.Random

actual object Bridge {
    actual fun uppercase(lower: String): String {
        return lower.uppercase()
    }

    actual fun byteArray(length: Int): ByteArray {
        return Random.nextBytes(length)
    }

    actual fun mixed(number: Long, value: CharArray, upper: Boolean, char: Char): ByteArray {
        return "$number$char${value.concatToString()}".let {
            if (upper) it.uppercase()
            else it
        }.encodeToByteArray()
    }

    actual fun withTypeAlias(long: TestAlias): TestAlias {
        return long
    }

    actual fun serializable(obj: ColorfulObject): String {
        return obj.color
    }
}