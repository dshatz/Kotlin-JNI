package kni.test.serializable

import com.dshatz.kni.annotations.JniSerializable
import kni.test.Callback

@JniSerializable
sealed interface PolymorphicFruit {
    data class Orange(val sweet: Boolean): PolymorphicFruit
    data class Apple(val color: Long): PolymorphicFruit
    data class Watermelon(val contents: ByteArray, val longs: LongArray, val chars: CharArray): PolymorphicFruit {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Watermelon

            if (!contents.contentEquals(other.contents)) return false
            if (!longs.contentEquals(other.longs)) return false
            if (!chars.contentEquals(other.chars)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = contents.contentHashCode()
            result = 31 * result + longs.contentHashCode()
            result = 31 * result + chars.contentHashCode()
            return result
        }
    }
}