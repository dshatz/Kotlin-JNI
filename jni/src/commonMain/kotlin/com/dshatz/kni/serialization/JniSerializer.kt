package com.dshatz.kni.serialization

import kotlinx.io.Buffer
import kotlinx.io.readByteArray

interface JniSerializer<T> {
    fun packTo(value: T, buffer: Buffer)
    fun unpackFrom(buffer: Buffer): T
}

fun <T> JniSerializer<T>.pack(value: T): ByteArray {
    val b = Buffer()
    packTo(value, b)
    return b.readByteArray()
}

fun <T> JniSerializer<T>.unpack(byteArray: ByteArray): T {
    val b = Buffer()
    b.write(byteArray)
    return unpackFrom(b)
}

fun Buffer.writeLenString(value: String) {
    val bytes = value.encodeToByteArray()
    writeInt(bytes.size)
    write(bytes)
}

fun Buffer.readLenString(): String {
    val len = readInt()
    return readByteArray(len).decodeToString()
}