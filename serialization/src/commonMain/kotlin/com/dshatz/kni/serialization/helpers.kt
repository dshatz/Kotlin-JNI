package com.dshatz.kni.serialization

import kotlinx.io.Buffer

fun <T> T.serialize(serializer: JniSerializer<T>): ByteArray {
    return serializer.pack(this)
}

fun <T> ByteArray.deserialize(serializer: JniSerializer<T>): T {
    return serializer.unpackFrom(this)
}

fun <T> Buffer.deserialize(serializer: JniSerializer<T>): T {
    return serializer.unpackFrom(this)
}