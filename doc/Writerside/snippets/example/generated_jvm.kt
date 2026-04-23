public actual fun mixed(
    number: Long,
    `value`: CharArray,
    upper: Boolean,
    char: Char,
): String = mixedExternal(number, `value`, upper, char)

public external fun mixedExternal(
    number: Long,
    `value`: CharArray,
    upper: Boolean,
    char: Char,
): String