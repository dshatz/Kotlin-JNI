package kni.test

import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.matchers.shouldBe
import kotlin.random.Random

fun TestSuiteScope.bridgeTests() {
    System.loadLibrary("e2e")
    test("Strings") {
        Bridge().uppercase("hello") shouldBe "HELLO"
    }
    test("ByteArray") {
        Bridge().byteArray(4).size shouldBe 4
    }

    test("mixed") {
        Bridge().apply {
            Bridge().mixed(Long.MAX_VALUE, " - max value".toCharArray(), false).decodeToString() shouldBe
                    "${Long.MAX_VALUE} - max value"
        }
    }
    test("negative long") {
        Bridge().mixed(Long.MIN_VALUE, " - negative value".toCharArray(), true).decodeToString() shouldBe
                "${Long.MIN_VALUE} - negative value".uppercase()
    }
    test("Alias type") {
        val value = Random.nextBytes(10).decodeToString()
        Bridge().withTypeAlias(value) shouldBe value
    }
}