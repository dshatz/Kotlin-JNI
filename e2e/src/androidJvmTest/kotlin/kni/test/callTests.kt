package kni.test

import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer
import kotlin.random.Random

fun TestSuiteScope.callTests() {
    try {
        System.loadLibrary("e2e")
        println("e2e.so loaded")
    } catch (e: Exception) {
        e.printStackTrace()
    }
    testFixture {
        println("Initialize caller")
        JvmCallerImpl().also { CallerBridge().init(it) }
    } closeWith {
        println("Dispose caller")
        close()
    } asParameterForEach {
        test("int") {
            CallerBridge().askJvmForANumber() shouldBe 11
        }
        test("direct ByteBuffer") {
            val buffer = ByteBuffer.allocateDirect(100)
            val expected = CallerBridge().askJvmToFillBuffer(buffer)
            val actual = ByteArray(expected.hexToByteArray().size).also {
                buffer.get(it)
            }
            println(actual.toHexString())
            actual.toHexString() shouldBe expected
        }
        test("Alias type") {
            val value = (1..10).map { ('a'..'z').random() }.joinToString("")
            CallerBridge().sendTypeAlias(value) shouldBe value
        }
    }
}

private class JvmCallerImpl : JvmCaller {
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