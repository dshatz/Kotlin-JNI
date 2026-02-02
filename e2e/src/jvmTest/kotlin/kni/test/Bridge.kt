package kni.test

import dev.datlag.nkommons.ByteBuffer

class Bridge {
    external fun uppercase(lower: String): String
    external fun byteArray(length: Int): ByteArray
    external fun mixed(number: Long, value: CharArray, upper: Boolean): ByteArray

    external fun createByteBuffer(): ByteBuffer
}