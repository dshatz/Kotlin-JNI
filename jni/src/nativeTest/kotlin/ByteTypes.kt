import de.infix.testBalloon.framework.core.testSuite
import dev.datlag.nkommons.binding.jbyte
import io.kotest.matchers.shouldBe

val ByteTypes by testSuite {
    testSuite("jbyte") {
        test("positive") {
            val a: jbyte = 2

            a shouldBe 2
        }
        test("negative") {
            val a: jbyte = -4

            a shouldBe -4
        }
    }
}