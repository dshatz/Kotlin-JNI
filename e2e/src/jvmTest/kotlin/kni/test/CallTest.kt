package kni.test

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer
import kotlin.random.Random

private val call = object : JvmCaller {
    override fun giveANumber(): Int {
        return 11
    }

    override fun fillBuffer(buffer: dev.datlag.nkommons.binding.ByteBuffer): String {
        val bytes = Random.nextBytes(buffer.jvmBuffer.capacity())
        buffer.jvmBuffer.put(bytes)
        return bytes.toHexString()
    }
}

external fun askJvmForANumber(caller: JvmCaller): Int
external fun askJvmToFillBuffer(caller: JvmCaller, buffer: ByteBuffer): String

class CallTest: DescribeSpec({
    describe("jvm -> native -> jvm") {
        it("int") {
            askJvmForANumber(call) shouldBe 11
        }
        it("direct ByteBuffer") {
            val buffer = ByteBuffer.allocateDirect(100)
            val expected = askJvmToFillBuffer(call, buffer)
            val actual = ByteArray(expected.hexToByteArray().size).also {
                buffer.get(it)
            }
            println(actual.toHexString())
            actual.toHexString() shouldBe expected
        }
    }
}) {
    init {
        System.loadLibrary("e2e")
    }
}