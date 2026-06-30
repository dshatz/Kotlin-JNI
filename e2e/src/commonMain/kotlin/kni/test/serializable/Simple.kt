package kni.test.serializable

import com.dshatz.kni.annotations.JniSerializable
import com.dshatz.kni.serialization.JniSerializer
import com.dshatz.kni.serialization.readLenString
import com.dshatz.kni.serialization.writeLenString
import kotlinx.io.Buffer
import kotlinx.io.files.Path

object PathSerializer: JniSerializer<Path> {
    override fun packTo(value: Path, buffer: Buffer) {
        buffer.writeLenString(value.toString())
    }

    override fun unpackFrom(buffer: Buffer): Path {
        return Path(buffer.readLenString())
    }
}

@JniSerializable
data class Simple(
    val byte: Byte,
    val short: Short,
    val int: Int,
    val long: Long,
    val float: Float,
    val double: Double,
    val string: String,
    val list: List<String>,
    val set: Set<String>,
    val map: Map<String, List<Long>>,
    val alias: LongAlias,
    val inner: Inner,
    val result: Result<String>,
    val resultOfCustom: Result<Inner>,
    @JniSerializable(PathSerializer::class) val path: Path
)

@JniSerializable
data class Inner(
    val byte: Byte
)

typealias LongAlias = Long