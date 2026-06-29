package kni.test.serializable

import com.dshatz.kni.annotations.JniSerializable
import kni.test.Callback

@JniSerializable
sealed interface PolymorphicFruit {
    data class Orange(val sweet: Boolean): PolymorphicFruit
    data class Apple(val color: Long): PolymorphicFruit
    /*data class Alive(val callback: Callback): PolymorphicFruit*/
}