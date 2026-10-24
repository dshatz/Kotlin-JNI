package kni.test

import com.dshatz.kni.annotations.JniAdapter
import com.dshatz.kni.annotations.JniCall

/**
 * String converted on native with [com.dshatz.kni.Encoding.UTF8].
 */
@JniAdapter(Utf8TextAdapter::class)
expect class Utf8Text(value: String) {
    val value: String
}

expect object Utf8TextAdapter: com.dshatz.kni.wrapper.JniAdapter<Utf8Text>

/**
 * String converted on native with [com.dshatz.kni.Encoding.UTF16].
 */
@JniAdapter(Utf16TextAdapter::class)
expect class Utf16Text(value: String) {
    val value: String
}

expect object Utf16TextAdapter: com.dshatz.kni.wrapper.JniAdapter<Utf16Text>

expect object EncodingBridge {
    /** Round trip: JVM -> native -> JVM. */
    @JniCall fun echoUtf8(text: Utf8Text): Utf8Text
    @JniCall fun echoUtf16(text: Utf16Text): Utf16Text

    /** UTF-16 code unit count as seen by native. Checks the JVM -> native direction on its own. */
    @JniCall fun lengthUtf8(text: Utf8Text): Int
    @JniCall fun lengthUtf16(text: Utf16Text): Int

    /** Text created on native. Checks the native -> JVM direction on its own. */
    @JniCall fun fromNativeUtf8(): Utf8Text
    @JniCall fun fromNativeUtf16(): Utf16Text
}
