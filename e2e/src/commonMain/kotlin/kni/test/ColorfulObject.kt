package kni.test

import com.dshatz.kni.annotations.JniSerializable
import com.dshatz.kni.annotations.JniSerializerFor
import com.dshatz.kni.serialization.JniSerializer
import com.dshatz.kni.serialization.JniWrappedException
import com.dshatz.kni.serialization.readLenString
import com.dshatz.kni.serialization.writeLenString
import kotlinx.io.Buffer

@JniSerializable
data class ColorfulObject(
    val color: String,
    val weight: Double,
    val property: IntRange,
)

@JniSerializerFor(IntRange::class)
object IntRangeSerializer: JniSerializer<IntRange> {
    override fun packTo(value: IntRange, buffer: Buffer) {
        buffer.writeInt(value.first)
        buffer.writeInt(value.last)
    }

    override fun unpackFrom(buffer: Buffer): IntRange {
        return IntRange(buffer.readInt(), buffer.readInt())
    }
}

@JniSerializerFor(Result::class)
class SimpleResultSerializer<T>(private val dataSerializer: JniSerializer<T>): JniSerializer<Result<T>> {
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
            return Result.failure(JniWrappedException(
                message,
                stackTrace
            ))
        }
    }
}