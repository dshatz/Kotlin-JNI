package kni.test.serializable

import com.dshatz.kni.annotations.JniSerializerFor
import com.dshatz.kni.serialization.JniSerializer
import kotlinx.io.Buffer

@JniSerializerFor(IntRange::class)
object IntRangeSerializer: JniSerializer<IntRange>("kotlin.ranges.IntRange") {
    override fun packToBuffer(value: IntRange, buffer: Buffer) {
        buffer.writeInt(value.first)
        buffer.writeInt(value.last)
    }

    override fun unpackFromBuffer(buffer: Buffer): IntRange {
        return IntRange(buffer.readInt(), buffer.readInt())
    }
}