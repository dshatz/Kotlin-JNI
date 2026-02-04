package kni.test

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.date.after
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

    override fun withTypeAlias(alias: TestAlias): TestAlias {
        return alias
    }

    override fun close() {
        // Clean up jvm-side resources.
        println("Closing JVM-side JVMCaller")
    }
}

external fun init(caller: JvmCaller)
external fun dispose()
external fun askJvmForANumber(): Int
external fun askJvmToFillBuffer(buffer: ByteBuffer): String
external fun sendTypeAlias(alias: TestAlias): TestAlias

class CallTest: DescribeSpec({
    describe("jvm -> native -> jvm") {
        beforeTest {
            println("Initializing")
            init(call)
        }
        afterTest { (case, result) ->
            println("Disposing after ${case.name.name}. Result: $result")
            dispose()
        }
        it("int") {
            askJvmForANumber() shouldBe 11
        }
        it("direct ByteBuffer") {
            val buffer = ByteBuffer.allocateDirect(100)
            val expected = askJvmToFillBuffer(buffer)
            val actual = ByteArray(expected.hexToByteArray().size).also {
                buffer.get(it)
            }
            println(actual.toHexString())
            actual.toHexString() shouldBe expected
        }
        it("Alias type") {
            val value = (1..10).map { ('a'..'z').random() }.joinToString("")
            sendTypeAlias(value) shouldBe value
        }
    }
}) {
    init {
        System.loadLibrary("e2e")
    }
}