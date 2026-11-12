package kni.test

import com.dshatz.kni.annotations.JniCall

actual class ChildOfAbstract actual constructor(private val value: Int) : AbstractBase(), AutoCloseable {
    @JniCall
    actual fun getValue(): Int = value

    @JniCall
    actual override fun describe(): String = "native($value)"

    actual override fun close() {}
}

actual object ObjectOfAbstract : AbstractBase() {
    @JniCall
    actual override fun describe(): String = "native object"
}
