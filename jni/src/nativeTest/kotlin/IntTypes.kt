import de.infix.testBalloon.framework.core.testSuite
import dev.datlag.nkommons.binding.jint
import dev.datlag.nkommons.binding.jsize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
val IntTypes by testSuite {
    testSuite("jint") {
        test("positive") {
            val a: jint = 42
            a shouldBe 42
        }

        test("negative") {
            val b: jint = -10
            b shouldBe -10
        }
    }
    testSuite("jsize") {
        test("default") {
            val size: jsize = 101
            size shouldBe 101
        }

        test("same as jint") {
            val size: jsize = 33
            val int: jint = 33

            size.should {
                it shouldBe int
                it::class shouldBe int::class
            }
        }
    }
}