package com.dshatz.kni.serialization

import kotlinx.io.Buffer


fun <T> Collection<T>.writeList(buffer: Buffer, writeItem: Buffer.(T) -> Unit) {
    buffer.writeInt(size)
    forEach {
        buffer.writeItem(it)
    }
}

inline fun <T> Buffer.readList(readItem: Buffer.() -> T): Collection<T> {
    val len = readInt()
    return (0..<len).map {
        readItem()
    }
}

fun <K, V> Map<K, V>.writeMap(buffer: Buffer, writeKey: Buffer.(K) -> Unit, writeValue: Buffer.(V) -> Unit) {
    buffer.writeInt(size)
    entries.forEach {
        buffer.writeKey(it.key)
        buffer.writeValue(it.value)
    }
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