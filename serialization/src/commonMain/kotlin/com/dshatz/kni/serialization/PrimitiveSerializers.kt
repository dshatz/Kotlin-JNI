package com.dshatz.kni.serialization

import kotlinx.io.Buffer
import kotlinx.io.readByteArray


fun <T> Collection<T>.writeList(buffer: Buffer, writeItem: Buffer.(T) -> Unit) {
    buffer.writeInt(size)
    forEach {
        buffer.writeItem(it)
    }
}

fun <T> Collection<T>.writeList(writeItem: Buffer.(T) -> Unit): ByteArray {
    val buffer = Buffer()
    buffer.writeInt(size)
    forEach {
        buffer.writeItem(it)
    }
    return buffer.readByteArray()
}

inline fun <T> Buffer.readList(readItem: Buffer.() -> T): Collection<T> {
    val len = readInt()
    return (0..<len).map {
        readItem()
    }
}

inline fun <T> ByteArray.readList(readItem: Buffer.() -> T): Collection<T> {
    val buffer = Buffer()
    buffer.write(this)
    val len = buffer.readInt()
    return (0..<len).map {
        readItem(buffer)
    }
}

fun <K, V> Map<K, V>.writeMap(buffer: Buffer, writeKey: Buffer.(K) -> Unit, writeValue: Buffer.(V) -> Unit) {
    buffer.writeInt(size)
    entries.forEach {
        buffer.writeKey(it.key)
        buffer.writeValue(it.value)
    }
}

fun <K, V> Map<K, V>.writeMap(writeKey: Buffer.(K) -> Unit, writeValue: Buffer.(V) -> Unit): ByteArray {
    val buffer = Buffer()
    buffer.writeInt(size)
    entries.forEach {
        buffer.writeKey(it.key)
        buffer.writeValue(it.value)
    }
    return buffer.readByteArray()
}

fun <K, V> Buffer.readMap(readKey: Buffer.() -> K, readValue: Buffer.() -> V): Map<K, V> {
    val len = readInt()
    return buildMap {
        repeat(len) {
            put(
                readKey(),
                readValue()
            )
        }
    }
}

fun <K, V> ByteArray.readMap(readKey: Buffer.() -> K, readValue: Buffer.() -> V): Map<K, V> {
    val buffer = Buffer()
    buffer.write(this)
    val len = buffer.readInt()
    return buildMap {
        repeat(len) {
            put(
                readKey(buffer),
                readValue(buffer)
            )
        }
    }
}