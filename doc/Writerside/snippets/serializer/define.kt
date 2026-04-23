import com.dshatz.kni.annotations.AddJniSerializer
import com.dshatz.kni.serialization.JniSerializer
import com.dshatz.kni.serialization.readLenString
import com.dshatz.kni.serialization.writeLenString
import kotlinx.io.Buffer
import kotlinx.io.readDouble
import kotlinx.io.writeDouble

data class ColorfulObject(
    val color: String,
    val weight: Double
)

@AddJniSerializer
object ColorfulObjectSerializer: JniSerializer<ColorfulObject> {
    override fun packTo(value: ColorfulObject, buffer: Buffer) {
        buffer.writeLenString(value.color)
        buffer.writeDouble(value.weight)
    }

    override fun unpackFrom(buffer: Buffer): ColorfulObject {
        return ColorfulObject(
            buffer.readLenString(),
            buffer.readDouble()
        )
    }
}