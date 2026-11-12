package com.dshatz.kni.buffers

/**
 * Encode this string as UTF-8 directly into a new buffer from [allocateBuffer].
 *
 * Unlike `encodeToByteArray()` followed by [ByteBuffer.write], no intermediate [ByteArray] holds a copy of the data,
 * which matters for sensitive strings such as passwords.
 *
 * Unpaired surrogates are encoded as U+FFFD (the replacement character).
 *
 * @param nullTerminated append a zero byte, as expected by C functions taking a `const char*`.
 * The terminator is included in [ByteBuffer.capacity].
 * @param wipeOnRelease see [allocateBuffer].
 */
fun String.encodeToByteBuffer(nullTerminated: Boolean = false, wipeOnRelease: Boolean = false): ByteBuffer {
    var length = if (nullTerminated) 1L else 0L
    forEachUtf8Byte { length++ }

    val buffer = allocateBuffer(length, wipeOnRelease)
    try {
        var index = 0L
        forEachUtf8Byte { buffer.setByte(index++, it) }
        if (nullTerminated) buffer.setByte(index, 0)
    } catch (e: Throwable) {
        buffer.release()
        throw e
    }
    return buffer
}

/**
 * Write a single byte at [index], without bounds checks beyond the platform's own.
 */
internal expect fun ByteBuffer.setByte(index: Long, value: Byte)

private inline fun String.forEachUtf8Byte(action: (Byte) -> Unit) {
    var i = 0
    while (i < length) {
        val c = this[i].code
        when {
            c < 0x80 -> action(c.toByte())
            c < 0x800 -> {
                action((0xC0 or (c shr 6)).toByte())
                action((0x80 or (c and 0x3F)).toByte())
            }
            c in 0xD800..0xDBFF && i + 1 < length && this[i + 1].code in 0xDC00..0xDFFF -> {
                val codePoint = 0x10000 + ((c - 0xD800) shl 10) + (this[i + 1].code - 0xDC00)
                action((0xF0 or (codePoint shr 18)).toByte())
                action((0x80 or ((codePoint shr 12) and 0x3F)).toByte())
                action((0x80 or ((codePoint shr 6) and 0x3F)).toByte())
                action((0x80 or (codePoint and 0x3F)).toByte())
                i++
            }
            c in 0xD800..0xDFFF -> {
                // Unpaired surrogate: U+FFFD
                action(0xEF.toByte())
                action(0xBF.toByte())
                action(0xBD.toByte())
            }
            else -> {
                action((0xE0 or (c shr 12)).toByte())
                action((0x80 or ((c shr 6) and 0x3F)).toByte())
                action((0x80 or (c and 0x3F)).toByte())
            }
        }
        i++
    }
}
