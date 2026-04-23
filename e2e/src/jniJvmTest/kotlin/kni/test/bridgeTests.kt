package kni.test

import com.dshatz.kni.buffers.allocateBuffer
import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.matchers.shouldBe
import com.dshatz.kni.load.BundledLibLoader

fun TestSuiteScope.bridgeTests() {
    BundledLibLoader.loadBundledLibrary("e2e")
    test("Strings") {
        Bridge.uppercase("hello") shouldBe "HELLO"
    }
    test("ByteArray") {
        Bridge.byteArray(4).size shouldBe 4
    }

    test("mixed") {
        Bridge.apply {
            mixed(Long.MAX_VALUE, " - max value".toCharArray(), false, 'x') shouldBe
                    "${Long.MAX_VALUE}x - max value"
        }
    }
    test("negative long") {
        Bridge.mixed(Long.MIN_VALUE, " - negative value".toCharArray(), true, 'x') shouldBe
                "${Long.MIN_VALUE}x - negative value".uppercase()
    }
    test("Alias type") {
        val value: TestAlias = generateSequence { "abc123".random() }.take(10).toList().toCharArray().concatToString()
        Bridge.withTypeAlias(value) shouldBe value
    }

    test("custom serializer") {
        val obj = ColorfulObject("green", 0.25)
        CommonCallable.makeOrange(obj).color shouldBe "orange"
    }

    test("callable buffer") {
        val buffer = allocateBuffer(1024)
        val filledHex = CommonCallable.fillBuffer(buffer)
        val contents = buffer.readToByteArray(0, 1024)
        contents.toHexString() shouldBe filledHex
    }
    test("top-level") {
        topLevelFun().capacity shouldBe 1024
    }
    test("zero copy buffer") {
        val buffer = topLevelFun() // allocated in native
        val filledHex = CommonCallable.fillBuffer(buffer) // pass it back to native and fill there
        val contents = buffer.readToByteArray(0, 1024)
        contents.toHexString() shouldBe filledHex // it's all the same underlying native memory!
    }
}