package com.dshatz.kni.serialization

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.readUShort
import kotlinx.io.writeUShort

interface JniSerializer<T> {
    fun packTo(value: T, buffer: Buffer)
    fun unpackFrom(buffer: Buffer): T

    fun unpackFrom(byteArray: ByteArray): T {
        val buffer = Buffer()
        buffer.write(byteArray)
        return unpackFrom(buffer)
    }

    fun pack(value: T): ByteArray {
        val buffer = Buffer()
        packTo(value, buffer)
        return buffer.readByteArray()
    }
}

fun Buffer.writeLenString(value: String) {
    val bytes = value.encodeToByteArray()
    writeInt(bytes.size)
    write(bytes)
}

fun Buffer.writeLenBytes(bytes: ByteArray) {
    writeInt(bytes.size)
    write(bytes)
}

fun Buffer.readLenBytes(): ByteArray {
    val count = readInt()
    return readByteArray(count)
}

fun Buffer.writeChar(char: Char) {
    writeUShort(char.code.toUShort())
}

fun Buffer.readChar(): Char {
    return readUShort().toInt().toChar()
}

fun Buffer.readLenString(): String {
    val len = readInt()
    return readByteArray(len).decodeToString()
}

fun Buffer.writeBool(value: Boolean) {
    writeByte(if (value) 1 else 0)
}

fun Buffer.readBool(): Boolean {
    return readByte() == 1.toByte()
}