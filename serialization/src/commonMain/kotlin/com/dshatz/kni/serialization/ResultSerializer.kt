package com.dshatz.kni.serialization

import com.dshatz.kni.serialization.exception.JniWrappedException
import kotlinx.io.Buffer

class ResultSerializer<T>(private val dataSerializer: JniSerializer<T>): JniSerializer<Result<T>> {
    override fun packTo(value: Result<T>, buffer: Buffer) {
        value.map {
            buffer.writeByte(1)
            dataSerializer.packTo(it, buffer)
            Unit
        }.getOrElse {
            buffer.writeByte(0)
            buffer.writeLenString(it.message.orEmpty())
            buffer.writeLenString(it.stackTraceToString())
        }
    }

    override fun unpackFrom(buffer: Buffer): Result<T> {
        val success = buffer.readByte() == 1.toByte()
        if (success) {
            return Result.success(dataSerializer.unpackFrom(buffer))
        } else {
            val message = buffer.readLenString()
            val stackTrace = buffer.readLenString()
            return Result.failure(
                JniWrappedException(
                    message,
                    stackTrace
                )
            )
        }
    }
}