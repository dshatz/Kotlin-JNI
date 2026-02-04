import de.infix.testBalloon.framework.core.testSuite
import dev.datlag.nkommons.binding.jchar
import dev.datlag.nkommons.utils.toJChar
import dev.datlag.nkommons.utils.toKChar
import io.kotest.matchers.shouldBe

val CharTypes by testSuite {
    testSuite("jchar") {
        test("latin") {
            val a: jchar = 106u

            a shouldBe 'j'.code.toUShort()
        }
        test("hiragana") {
            val e: jchar = 12360u

            e shouldBe 'え'.code.toUShort()
        }
        test("katakana") {
            val fu: jchar = 12501u

            fu shouldBe 'フ'.code.toUShort()
        }
    }
    testSuite("utils") {
        test("toKChar") {
            val a: jchar = 106u

            a.toKChar() shouldBe 'j'
        }
        test("toJChar") {
            val b: Char = 'j'

            b.toJChar() shouldBe 106u
        }
    }
}