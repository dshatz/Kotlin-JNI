package kni.test

import com.dshatz.kni.buffers.allocateBuffer
import com.dshatz.kni.load.BundledLibLoader
import com.dshatz.kni.serialization.JniWrappedException
import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

fun TestSuiteScope.callTests() {
    try {
        BundledLibLoader.loadBundledLibrary("e2e")
        println("e2e.so loaded")
    } catch (e: Exception) {
        e.printStackTrace()
    }
    testFixture {
        println("Initialize caller")
        JvmCallerImpl().also {
            CallerBridge.init(it)
            println("Initialized")
        }
    } closeWith {
        println("Dispose caller")
        close()
    } asParameterForEach {
        test("int") {
            CallerBridge.askJvmForANumber() shouldBe 11
        }
        test("direct ByteBuffer") {
            val buffer = allocateBuffer(100)
            val expected = CallerBridge.askJvmToFillBuffer(buffer)
            val actual = ByteArray(expected.hexToByteArray().size).also {
                buffer.read(it, 0, buffer.capacity.toInt())
            }
            println(actual.toHexString())
            actual.toHexString() shouldBe expected
        }
        test("Alias type") {
            val value = (1..10).map { ('a'..'z').random() }.joinToString("")
            CallerBridge.sendTypeAlias(value) shouldBe value
        }
        test("threaded") {
            // Initialize on test scope, call on test scope and other threads.
            CallerBridge.askJvmForANumber() shouldBe 11
            withContext(Dispatchers.Default) {
                CallerBridge.askJvmForANumber() shouldBe 11
            }
            withContext(Dispatchers.IO) {
                CallerBridge.askJvmForANumber() shouldBe 11
                CallerBridge.askJvmForANumber() shouldBe 11
            }
        }
        test("spam native callbacks") {
            withContext(Dispatchers.IO) {
                withTimeout(2.seconds) {
                    (1..10).map {
                        async {
                            suspendCancellableCoroutine { cont ->
                                CallerBridge.callbackFromCoroutine(object : Callback {
                                    override fun onComplete(result: Boolean) {
                                        println("Received result! $result; Thread = ${Thread.currentThread().name}")
                                        cont.resume(result)
                                    }

                                    override fun close() {}

                                }, "Coroutine $it")
                            } shouldBe true
                        }
                    }.awaitAll()
                }
            }
        }

        test("result") {
            CallerBridge.giveResult(10, false) shouldBe Result.success(10)
            val error = CallerBridge.giveResult(10, true)
            error.isFailure shouldBe true
            error.exceptionOrNull()?.message shouldContain "Native error"
            println(error.exceptionOrNull()?.printStackTrace())
            error.exceptionOrNull()?.stackTraceToString() shouldContain "com.dshatz.kni.serialization.JniWrappedException: Native error"
        }
    }
}

private class JvmCallerImpl : JvmCaller {
    override fun giveANumber(): Int {
        println(Thread.currentThread().name)
        return 11
    }

    override fun fillBuffer(buffer: com.dshatz.kni.buffers.ByteBuffer): String {
        val bytes = Random.nextBytes(buffer.capacity.toInt())
        buffer.write(bytes)
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