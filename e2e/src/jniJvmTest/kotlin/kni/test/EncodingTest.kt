package kni.test

import com.dshatz.kni.load.BundledLibLoader
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe

private const val BMP_TEXT = "héllo ✓ 日本語"
private const val EMOJI_TEXT = "smile 😀, thumbs 👍🏽, family 👨‍👩‍👧, flag 🇱🇻, clef 𝄞"
private const val NUL_TEXT = "a\u0000b"

/** Mirrors NATIVE_TEXT in EncodedText.jniNative.kt. */
private const val NATIVE_TEXT_EXPECTED = "native 😀 日本語"

val EncodingTest by testSuite {
    BundledLibLoader.loadBundledLibrary("e2e_adapters")
    BundledLibLoader.loadBundledLibrary("e2e")

    test("UTF16 BMP round trip") {
        EncodingBridge.echoUtf16(Utf16Text(BMP_TEXT)).value shouldBe BMP_TEXT
    }
    test("UTF16 emoji round trip") {
        EncodingBridge.echoUtf16(Utf16Text(EMOJI_TEXT)).value shouldBe EMOJI_TEXT
    }
    test("UTF16 NUL round trip") {
        EncodingBridge.echoUtf16(Utf16Text(NUL_TEXT)).value shouldBe NUL_TEXT
    }
    test("UTF16 JVM to native") {
        EncodingBridge.lengthUtf16(Utf16Text(EMOJI_TEXT)) shouldBe EMOJI_TEXT.length
        EncodingBridge.lengthUtf16(Utf16Text(NUL_TEXT)) shouldBe NUL_TEXT.length
    }
    test("UTF16 native to JVM") {
        EncodingBridge.fromNativeUtf16().value shouldBe NATIVE_TEXT_EXPECTED
    }
    test("UTF16 empty") {
        EncodingBridge.echoUtf16(Utf16Text("")).value shouldBe ""
    }

    test("UTF8 BMP round trip") {
        EncodingBridge.echoUtf8(Utf8Text(BMP_TEXT)).value shouldBe BMP_TEXT
    }
    test("UTF8 emoji round trip") {
        EncodingBridge.echoUtf8(Utf8Text(EMOJI_TEXT)).value shouldBe EMOJI_TEXT
    }
    test("UTF8 NUL round trip") {
        EncodingBridge.echoUtf8(Utf8Text(NUL_TEXT)).value shouldBe NUL_TEXT
    }
    test("UTF8 JVM to native") {
        EncodingBridge.lengthUtf8(Utf8Text(EMOJI_TEXT)) shouldBe EMOJI_TEXT.length
        EncodingBridge.lengthUtf8(Utf8Text(NUL_TEXT)) shouldBe NUL_TEXT.length
    }
    test("UTF8 native to JVM") {
        EncodingBridge.fromNativeUtf8().value shouldBe NATIVE_TEXT_EXPECTED
    }
    test("UTF8 empty") {
        EncodingBridge.echoUtf8(Utf8Text("")).value shouldBe ""
    }
}
