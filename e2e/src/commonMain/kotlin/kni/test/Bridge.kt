package kni.test

import com.dshatz.kni.annotations.Callable


expect object Bridge {
    @Callable fun uppercase(lower: String): String
    @Callable fun byteArray(length: Int): ByteArray
    @Callable fun mixed(number: Long, value: CharArray, upper: Boolean, char: Char): ByteArray
    @Callable fun withTypeAlias(long: TestAlias): TestAlias
    @Callable fun serializable(obj: ColorfulObject): String
}