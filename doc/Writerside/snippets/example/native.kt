// commonMain

@JniCall
expect fun mixed(number: Long, value: CharArray, upper: Boolean, char: Char): String

// nativeMain or jniNativeMain
actual fun mixed(number: Long, value: CharArray, upper: Boolean, char: Char): String
    return "$a, $b, $c, $d"
}