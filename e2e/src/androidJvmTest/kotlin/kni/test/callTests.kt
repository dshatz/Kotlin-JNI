package kni.test

import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import com.dshatz.kni.load.BundledLibLoader

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
            CallerBridge().init(it)
            println("Initialized")
        }
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
        test("threaded") {
            // Initialize on test scope, call on test scope and other threads.
            CallerBridge().askJvmForANumber() shouldBe 11
            withContext(Dispatchers.Default) {
                CallerBridge().askJvmForANumber() shouldBe 11
            }
            withContext(Dispatchers.IO) {
                CallerBridge().askJvmForANumber() shouldBe 11
                CallerBridge().askJvmForANumber() shouldBe 11
            }
        }
        test("spam native callbacks") {
            withContext(Dispatchers.IO) {
                withTimeout(2.seconds) {
                    (1..10).map {
                        async {
                            suspendCancellableCoroutine { cont ->
                                CallerBridge().callbackFromCoroutine(object : Callback {
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
    }
}

private class JvmCallerImpl : JvmCaller {
    override fun giveANumber(): Int {
        println(Thread.currentThread().name)
        return 11
    }

    override fun fillBuffer(buffer: com.dshatz.kni.buffers.ByteBuffer): String {
        val bytes = Random.nextBytes(buffer.capacity.toInt())
        buffer.put(bytes)
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