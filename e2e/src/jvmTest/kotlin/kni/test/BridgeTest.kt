package kni.test

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class BridgeTest: DescribeSpec({
    describe("Bridge test") {
        it("Strings") {
            Bridge().uppercase("hello") shouldBe "HELLO"
        }

        it("ByteArray") {
            Bridge().byteArray(4).size shouldBe 4
        }
        it("mixed") {
            Bridge().apply {
                mixed(Long.MAX_VALUE, " - max value".toCharArray(), false).decodeToString() shouldBe
                        "${Long.MAX_VALUE} - max value"
            }
        }
        it("negative long") {
            Bridge().mixed(Long.MIN_VALUE, " - negative value".toCharArray(), true).decodeToString() shouldBe
                    "${Long.MIN_VALUE} - negative value".uppercase()
        }
        it("Alias type") {
            val value = Random.nextBytes(10).decodeToString()
            Bridge().withTypeAlias(value) shouldBe value
        }
    }
}) {
    init {
        System.loadLibrary("e2e")
    }
}