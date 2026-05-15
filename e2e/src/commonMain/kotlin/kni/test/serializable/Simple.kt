package kni.test.serializable

import com.dshatz.kni.annotations.JniSerializable

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
    val resultOfCustom: Result<Inner>
)

@JniSerializable
data class Inner(
    val byte: Byte
)

typealias LongAlias = Long