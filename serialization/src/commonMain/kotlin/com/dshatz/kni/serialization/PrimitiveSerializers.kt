package com.dshatz.kni.serialization

import kotlinx.io.Buffer

fun <T> Buffer.writeList(col: Collection<T>, writeItem: Buffer.(T) -> Unit) {
    writeInt(col.size)
    col.forEach {
        writeItem(it)
    }
}

inline fun <T> Buffer.readList(readItem: Buffer.() -> T): Collection<T> {
    val len = readInt()
    return (0..<len).map {
        readItem()
    }
}

fun <K, V> Buffer.writeMap(map: Map<K, V>, writeKey: Buffer.(K) -> Unit, writeValue: Buffer.(V) -> Unit) {
    writeInt(map.size)
    map.entries.forEach {
        writeKey(this@writeMap, it.key)
        writeValue(this@writeMap, it.value)
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