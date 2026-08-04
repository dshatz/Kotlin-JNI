package kni.test.serializable

import com.dshatz.kni.annotations.JniSerializable

@JniSerializable
data class ColorfulObject(
    val color: String,
    val weight: Double,
    val property: IntRange,
)