package kni.test

import com.dshatz.kni.annotations.JniAdapter
import com.dshatz.kni.wrapper.JvmJniAdapter

@JniAdapter(Utf8TextAdapter::class)
actual class Utf8Text actual constructor(actual val value: String)

actual object Utf8TextAdapter : JvmJniAdapter<Utf8Text, String> {
    override fun getJniValue(value: Utf8Text): String = value.value
    override fun fromJniValue(value: String): Utf8Text = Utf8Text(value)
}

@JniAdapter(Utf16TextAdapter::class)
actual class Utf16Text actual constructor(actual val value: String)

actual object Utf16TextAdapter : JvmJniAdapter<Utf16Text, String> {
    override fun getJniValue(value: Utf16Text): String = value.value
    override fun fromJniValue(value: String): Utf16Text = Utf16Text(value)
}
