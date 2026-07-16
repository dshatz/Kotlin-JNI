import com.dshatz.kni.serialization.JniSerializable

@JniSerializable
data class ColorfulObject(
    val color: String,
    val weight: Double
)

@JniSerializable
value class Amount(val amountCents: Long) {
    val amount: Double get() = amountCents / 100.0
}