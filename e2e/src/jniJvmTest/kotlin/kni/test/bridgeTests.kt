package kni.test

import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.buffers.allocateBuffer
import com.dshatz.kni.load.BundledLibLoader
import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kni.test.flows.NativeObjWithFlow
import kni.test.serializable.ColorfulObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.pow

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
        val obj = ColorfulObject("green", 0.25, 0..100)
        CommonCallable.makeOrange(obj).color shouldBe "orange"
    }

    test("callable buffer") {
        val buffer = allocateBuffer(1024)
        val filledHex = CommonCallable.fillBuffer(buffer)
        val contents = buffer.readToByteArray(0, 1024)
        contents.toHexString() shouldBe filledHex
    }
    test("LongArray parameter and return value") {
        val value = longArrayOf(-2, 0, 1, 42)

        CommonCallable.longArray(value).contentEquals(longArrayOf(-4, 0, 2, 84)) shouldBe true
    }
    test("ByteArray parameter and return value") {
        val value = byteArrayOf(-128, -1, 0, 1, 127)

        CommonCallable.byteArray(value).contentEquals(byteArrayOf(127, 1, 0, -1, -128)) shouldBe true
    }
    test("ShortArray parameter and return value") {
        val value = shortArrayOf(-128, -1, 0, 1, 127)

        CommonCallable.shortArray(value).contentEquals(shortArrayOf(127, 1, 0, -1, -128)) shouldBe true
    }
    test("BooleanArray parameter and return value") {
        val value = booleanArrayOf(true, false, false, true)

        CommonCallable.booleanArray(value).contentEquals(booleanArrayOf(true, false, false, true)) shouldBe true
    }
    test("CharArray parameter and return value") {
        val value = charArrayOf('a', 'b', 'c', 'd')

        CommonCallable.charArray(value).contentEquals(charArrayOf('d', 'c', 'b', 'a')) shouldBe true
    }
    test("DoubleArray parameter and return value") {
        val value = doubleArrayOf(-2.5, 0.0, 1.25, 42.0)

        CommonCallable.doubleArray(value).contentEquals(doubleArrayOf(-5.0, 0.0, 2.5, 84.0)) shouldBe true
    }
    test("FloatArray parameter and return value") {
        val value = floatArrayOf(-2.5f, 0f, 1.25f, 42f)

        CommonCallable.floatArray(value).contentEquals(floatArrayOf(-5f, 0f, 2.5f, 84f)) shouldBe true
    }
    test("IntArray parameter and return value") {
        val value = intArrayOf(-2, 0, 1, 42)

        CommonCallable.intArray(value).contentEquals(intArrayOf(-4, 0, 2, 84)) shouldBe true
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
    test("native instance 2 constructors") {
        val obj1 = NativeInstance(100, NoopCloseListener())
        obj1.negate() shouldBe -100

        val obj2 = NativeInstance(NoopCloseListener(), 200)
        obj2.negate() shouldBe -200
    }
    test("native instance") {
        val obj = NativeInstance1(100)
        obj.getInput() shouldBe 100
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

    test("nullable primitives") {
        CommonCallable.anyIsNull() shouldBe false
        CommonCallable.anyIsNull(str = null) shouldBe true
        CommonCallable.anyIsNull(i = null) shouldBe true
        CommonCallable.anyIsNull(f = null) shouldBe true
        CommonCallable.anyIsNull(d = null) shouldBe true
        CommonCallable.anyIsNull(c = null) shouldBe true
        CommonCallable.anyIsNull(short = null) shouldBe true
        CommonCallable.anyIsNull(byte = null) shouldBe true
        CommonCallable.anyIsNull(byteArray = null) shouldBe true
        CommonCallable.anyIsNull(charArray = null) shouldBe true
        CommonCallable.anyIsNull(floatArray = null) shouldBe true
        CommonCallable.anyIsNull(doubleArray = null) shouldBe true
        CommonCallable.anyIsNull(intArray = null) shouldBe true
        CommonCallable.anyIsNull(serializable = null) shouldBe true
        CommonCallable.anyIsNull(buffer = null) shouldBe true
        CommonCallable.anyIsNull(callback = null) shouldBe true
    }

    test("exception") {
        val callback = object: ThrowingCallback {
            override fun unstable(shouldThrow: Boolean) {
                if (shouldThrow) error("Error from jvm callback")
            }

            override fun nullable(
                shouldThrow: Boolean,
                returnNull: Boolean
            ): String? {
                if (shouldThrow) error("Error from nullable jvm callback") else {
                    return if (returnNull) null
                    else "value"
                }
            }

            override fun close() {
                TODO("Not yet implemented")
            }

        }
        CommonCallable.callThrowingCallback(callback, true)
        CommonCallable.getError() shouldBe "JVM Exception: java.lang.IllegalStateException(Error from jvm callback)"
        CommonCallable.callThrowingCallback(callback, false) // should clear the error
        CommonCallable.getError() shouldBe null

        CommonCallable.callThrowingNullableCallback(callback, true, false) shouldBe null
        CommonCallable.getError() shouldBe "JVM Exception: java.lang.IllegalStateException(Error from nullable jvm callback)"
        CommonCallable.callThrowingNullableCallback(callback, false, false) shouldBe "value"
        CommonCallable.getError() shouldBe null
        CommonCallable.callThrowingNullableCallback(callback, false, true) shouldBe null
        CommonCallable.getError() shouldBe null
    }

    test("flow of int") {
        val withFlow = NativeObjWithFlow()
        withFlow.myFlow.value shouldBe 11 // as initialized in defined actual in native code.
        withFlow.increment(2)
        withFlow.myFlow.value shouldBe 13
        withFlow.doubleAndGet() shouldBe withFlow.myFlow.value shouldBe 26
    }
    test("flow of serializable") {
        val withFlow = NativeObjWithFlow()
        withFlow.objectFlow.value shouldBe null
    }

    test("init on different thread") {
        val withFlow = withContext(Dispatchers.Default) {
            NativeObjWithFlow()
        }
        withContext(Dispatchers.IO) {
            (1..10).map {
                launch {
                    withFlow.doubleAndGet()
                }
            }.joinAll()
        }
        withFlow.myFlow.value shouldBe 11 * 2.0.pow(10.0)
    }

    test("close and overwrite") {
        val a = NativeInstance1(100)
        a.getInput() shouldBe 100
        a.close()
        shouldThrow<IllegalStateException> {
            a.getInput()
        }.message shouldBe "NativeInstance1 is closed."
        val b = NativeInstance1(200)
        b.getInput() shouldBe 200
    }
}

class NoopCloseListener: CloseListener {
    override fun onClose() {}

    override fun close() {}

}
