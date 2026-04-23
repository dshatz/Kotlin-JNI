package kni.test

import com.dshatz.kni.annotations.JniCall


expect object Bridge {
    @JniCall fun uppercase(lower: String): String
    @JniCall fun byteArray(length: Int): ByteArray
    @JniCall fun mixed(number: Long, value: CharArray, upper: Boolean, char: Char): String
    @JniCall fun withTypeAlias(long: TestAlias): TestAlias
    @JniCall fun serializable(obj: ColorfulObject): String
}