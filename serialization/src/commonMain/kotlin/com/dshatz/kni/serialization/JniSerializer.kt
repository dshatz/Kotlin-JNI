package com.dshatz.kni.serialization

import kotlinx.io.Buffer
import kotlinx.io.EOFException
import kotlinx.io.readByteArray
import kotlinx.io.readUShort
import kotlinx.io.writeUShort

abstract class JniSerializer<T>(private val descriptor: String) {
    protected abstract fun packToBuffer(value: T, buffer: Buffer)
    protected abstract fun unpackFromBuffer(buffer: Buffer): T

    /*fun packSafe(value: T, buffer: Buffer) {
        try {
            packTo(value, buffer)
        } catch (e: Exception) {
            throw RuntimeException("Could not pack $value: ${e.message}", e)
        }
    }
    fun unpackSafe(buffer: Buffer): T {
        try {
            unpackFrom(buffer)
        } catch (e: Exception) {
            throw RuntimeException("Could not unpack to ${T::class.simpleName}")
        }
    }*/

    fun unpackFrom(byteArray: ByteArray): T {
        val buffer = Buffer()
        buffer.write(byteArray)
        return unpackFrom(buffer)
    }

    fun packTo(value: T, buffer: Buffer) {
        try {
            packToBuffer(value, buffer)
        } catch (e: Exception) {
            throw RuntimeException("Could not pack $value: ${e.message}", e)
        }
    }

    fun unpackFrom(buffer: Buffer): T {
        return try {
            unpackFromBuffer(buffer)
        } catch (e: Exception) {
            throw RuntimeException("Could not unpack $descriptor: ${e.message}", e)
        }
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

fun Buffer.writeUnit(u: Unit) {}
fun Buffer.readUnit() {}

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