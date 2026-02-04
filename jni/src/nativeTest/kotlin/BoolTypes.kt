import de.infix.testBalloon.framework.core.testSuite
import dev.datlag.nkommons.binding.jboolean
import dev.datlag.nkommons.utils.toJBoolean
import dev.datlag.nkommons.utils.toKBoolean
import io.kotest.matchers.shouldBe
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toByte

@OptIn(ExperimentalForeignApi::class)
val BoolTypes by testSuite {
    testSuite("jboolean") {
        test("positive") {
            val a: jboolean = true.toByte().toUByte()

            a shouldBe 1u
        }
        test("negative") {
            val b: jboolean = false.toByte().toUByte()

            b shouldBe 0u
        }
    }
    testSuite("utils") {
        test("positive toKBoolean") {
            val a: jboolean = 1u

            a.toKBoolean() shouldBe true
        }
        test("negative toKBoolean") {
            val b: jboolean = 0u

            b.toKBoolean() shouldBe false
        }

        test("positive toJBoolean") {
            val a: Boolean = true

            a.toJBoolean() shouldBe 1u
        }
        test("negative toJBoolean") {
            val b: Boolean = false

            b.toJBoolean() shouldBe 0u
        }
    }
}