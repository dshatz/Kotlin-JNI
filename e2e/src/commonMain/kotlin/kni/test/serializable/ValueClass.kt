package kni.test.serializable

import com.dshatz.kni.annotations.JniSerializable
import kotlin.jvm.JvmInline

@JniSerializable
@JvmInline
value class ValueClass(val value: Int) {
    val prop1: String get() = value.toString()
}