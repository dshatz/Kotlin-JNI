package com.dshatz.kni

/**
 * Codec for the "modified UTF-8" used by JNI's `*StringUTF*` functions.
 *
 * Differs from standard UTF-8 in two ways:
 * - `\u0000` is encoded as the two bytes `C0 80`, so the result never contains a zero byte.
 * - Supplementary characters are encoded as two 3-byte sequences, one per UTF-16 surrogate.
 *
 * Both follow from encoding each UTF-16 code unit separately, which is what this codec does.
 */
internal object ModifiedUtf8 {

    fun encodedLength(value: String): Int {
        var size = 0
        for (c in value) {
            size += when (c.code) {
                in 0x01..0x7F -> 1
                in 0x00..0x7FF -> 2
                else -> 3
            }
        }
        return size
    }

    /**
     * @param nullTerminated append a trailing zero byte, as expected by `NewStringUTF`.
     */
    fun encode(value: String, nullTerminated: Boolean = false): ByteArray {
        val out = ByteArray(encodedLength(value) + if (nullTerminated) 1 else 0)
        var i = 0
        for (c in value) {
            val code = c.code
            when (code) {
                in 0x01..0x7F -> out[i++] = code.toByte()
                in 0x00..0x7FF -> {
                    out[i++] = (0xC0 or (code shr 6)).toByte()
                    out[i++] = (0x80 or (code and 0x3F)).toByte()
                }
                else -> {
                    out[i++] = (0xE0 or (code shr 12)).toByte()
                    out[i++] = (0x80 or ((code shr 6) and 0x3F)).toByte()
                    out[i++] = (0x80 or (code and 0x3F)).toByte()
                }
            }
        }
        return out
    }

    /**
     * Decodes the first [length] bytes of [bytes]. Input is expected to come from the JVM, so it is not validated.
     */
    fun decode(bytes: ByteArray, length: Int = bytes.size): String {
        val out = CharArray(length)
        var n = 0
        var i = 0
        while (i < length) {
            val b = bytes[i].toInt() and 0xFF
            when {
                b < 0x80 -> {
                    out[n++] = b.toChar()
                    i += 1
                }
                b and 0xE0 == 0xC0 -> {
                    out[n++] = (((b and 0x1F) shl 6) or (bytes[i + 1].toInt() and 0x3F)).toChar()
                    i += 2
                }
                b and 0xF0 == 0xE0 -> {
                    out[n++] = (((b and 0x0F) shl 12) or
                            ((bytes[i + 1].toInt() and 0x3F) shl 6) or
                            (bytes[i + 2].toInt() and 0x3F)).toChar()
                    i += 3
                }
                else -> {
                    // Standard 4-byte UTF-8, produced by ART for supplementary characters.
                    val codePoint = ((b and 0x07) shl 18) or
                            ((bytes[i + 1].toInt() and 0x3F) shl 12) or
                            ((bytes[i + 2].toInt() and 0x3F) shl 6) or
                            (bytes[i + 3].toInt() and 0x3F)
                    val offset = codePoint - 0x10000
                    out[n++] = (0xD800 or (offset shr 10)).toChar()
                    out[n++] = (0xDC00 or (offset and 0x3FF)).toChar()
                    i += 4
                }
            }
        }
        return out.concatToString(0, n)
    }
}
