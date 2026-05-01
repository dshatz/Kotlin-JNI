package kni.test

import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.buffers.allocateBuffer
import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.matchers.shouldBe
import com.dshatz.kni.load.BundledLibLoader
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    test("native instance") {
        val obj = NativeInstance(100, NoopCloseListener())
        obj.negate() shouldBe -100
    }

    test("native instance with callback") {
        val obj = NativeInstance(99, NoopCloseListener())
        val buffer = allocateBuffer(1024)
        val filledResult = suspendCancellableCoroutine {
            obj.fillBuffer(buffer, object : BufferFillCallback {
                override fun onFilled(buffer: ByteBuffer, hex: String) {
                    it.resume(buffer to hex)
                }

                override fun close() {

                }
            })
        }
        val (returnedBuffer, returnedHex) = filledResult
        returnedBuffer shouldBe buffer
        val data = returnedBuffer.readToByteArray(0, returnedBuffer.capacity.toInt())
        data.toHexString() shouldBe returnedHex
        returnedHex shouldBe ByteArray(1024).also { it.fill(99.toByte()) }.toHexString()
    }

    test("native instance close") {
        var closed = false
        val obj = NativeInstance(-99, object: CloseListener {
            override fun onClose() {
                closed = true
            }
            override fun close() {}
        })
        obj.negate() shouldBe 99
        obj.close()
        closed shouldBe true
    }
}

class NoopCloseListener: CloseListener {
    override fun onClose() {}

    override fun close() {}

}