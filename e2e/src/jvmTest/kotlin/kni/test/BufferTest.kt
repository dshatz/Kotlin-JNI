package kni.test

import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.buffers.allocateBuffer
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.lang.management.BufferPoolMXBean
import java.lang.management.ManagementFactory
import java.lang.ref.WeakReference

private fun directMemoryUsed() = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean::class.java)
    .first { it.name == "direct" }.memoryUsed

private fun gc() = repeat(3) { System.gc(); Thread.sleep(100) }

// 200MB would stay reachable if each buffer parameter was pinned by a global ref that is never deleted.
private const val LEAK_THRESHOLD = 50L * 1024 * 1024

val BufferTest by testSuite {
    bufferLifetimeTests()

    test("buffer parameters do not leak") {
        gc()
        val before = directMemoryUsed()
        repeat(200) {
            BufferKeeper.nullableBuffer(allocateBuffer(1024 * 1024))
        }
        gc()
        (directMemoryUsed() - before) shouldBeLessThan LEAK_THRESHOLD
    }

    test("constructor buffer parameters do not leak") {
        gc()
        val before = directMemoryUsed()
        repeat(200) {
            BufferHolder(allocateBuffer(1024 * 1024), retain = false).close()
        }
        gc()
        (directMemoryUsed() - before) shouldBeLessThan LEAK_THRESHOLD
    }

    test("suspend buffer parameters do not leak") {
        gc()
        val before = directMemoryUsed()
        repeat(200) {
            BufferKeeper.nullableBufferSuspend(allocateBuffer(1024 * 1024))
        }
        // Global refs are deleted asynchronously, after each call has returned to the JVM.
        eventually {
            gc()
            directMemoryUsed() - before < LEAK_THRESHOLD
        } shouldBe true
    }

    test("suspend buffer parameter is kept alive until the call completes") {
        var buffer: ByteBuffer? = allocateBuffer(1024 * 1024)
        val weakBuffer = WeakReference(buffer!!.jvmBuffer)
        coroutineScope {
            val call = async(Dispatchers.Default) {
                // Give up the only strong reference held by the test.
                val arg = buffer!!
                buffer = null
                BufferKeeper.fillAfterDelay(arg, 3, delayMillis = 1000)
            }
            delay(300)
            gc()
            // Native is still waiting to write to the buffer, it must not have been collected.
            weakBuffer.get().shouldNotBeNull()
            call.await()
        }
        eventually {
            gc()
            weakBuffer.get() == null
        } shouldBe true
    }
}
