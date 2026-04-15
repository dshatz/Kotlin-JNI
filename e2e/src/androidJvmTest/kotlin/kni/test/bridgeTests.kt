package kni.test

import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.matchers.shouldBe
import com.dshatz.kni.load.BundledLibLoader

fun TestSuiteScope.bridgeTests() {
    BundledLibLoader.loadBundledLibrary("e2e")
    test("Strings") {
        Bridge().uppercase("hello") shouldBe "HELLO"
    }
    test("ByteArray") {
        Bridge().byteArray(4).size shouldBe 4
    }

    test("mixed") {
        Bridge().apply {
            mixed(Long.MAX_VALUE, " - max value".toCharArray(), false).decodeToString() shouldBe
                    "${Long.MAX_VALUE} - max value"
        }
    }
    test("negative long") {
        Bridge().mixed(Long.MIN_VALUE, " - negative value".toCharArray(), true).decodeToString() shouldBe
                "${Long.MIN_VALUE} - negative value".uppercase()
    }
    test("Alias type") {
        val value: TestAlias = generateSequence { "abc123".random() }.take(10).toList().toCharArray().concatToString()
        Bridge().withTypeAlias(value) shouldBe value
    }
}