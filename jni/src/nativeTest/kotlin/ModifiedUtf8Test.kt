import com.dshatz.kni.ModifiedUtf8
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe

private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

val ModifiedUtf8Test by testSuite {
    testSuite("encode") {
        test("ascii") {
            ModifiedUtf8.encode("Az09") shouldBe "Az09".encodeToByteArray()
        }
        test("two byte") {
            ModifiedUtf8.encode("é") shouldBe bytes(0xC3, 0xA9)
        }
        test("three byte") {
            ModifiedUtf8.encode("日") shouldBe bytes(0xE6, 0x97, 0xA5)
        }
        test("NUL is C0 80") {
            ModifiedUtf8.encode("a\u0000b") shouldBe bytes(0x61, 0xC0, 0x80, 0x62)
        }
        test("emoji is two 3-byte surrogates") {
            // U+1F600 = D83D DE00
            ModifiedUtf8.encode("😀") shouldBe bytes(0xED, 0xA0, 0xBD, 0xED, 0xB8, 0x80)
        }
        test("null terminated") {
            ModifiedUtf8.encode("a\u0000", nullTerminated = true) shouldBe bytes(0x61, 0xC0, 0x80, 0x00)
        }
        test("encodedLength") {
            ModifiedUtf8.encodedLength("a\u0000é日😀") shouldBe 1 + 2 + 2 + 3 + 6
        }
    }
    testSuite("decode") {
        test("NUL") {
            ModifiedUtf8.decode(bytes(0x61, 0xC0, 0x80, 0x62)) shouldBe "a\u0000b"
        }
        test("emoji") {
            ModifiedUtf8.decode(bytes(0xED, 0xA0, 0xBD, 0xED, 0xB8, 0x80)) shouldBe "😀"
        }
        test("emoji as standard 4-byte UTF-8") {
            ModifiedUtf8.decode(bytes(0xF0, 0x9F, 0x98, 0x80)) shouldBe "😀"
        }
        test("ignores bytes past length") {
            ModifiedUtf8.decode(bytes(0x61, 0x62, 0x00), length = 2) shouldBe "ab"
        }
        test("empty") {
            ModifiedUtf8.decode(ByteArray(0)) shouldBe ""
        }
    }
    test("round trip") {
        val value = "héllo ✓ 日本語 😀👍🏽👨‍👩‍👧🇱🇻 𝄞 a\u0000b"
        ModifiedUtf8.decode(ModifiedUtf8.encode(value)) shouldBe value
    }
}
