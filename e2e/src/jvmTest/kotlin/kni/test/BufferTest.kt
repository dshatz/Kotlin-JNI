package kni.test

import com.dshatz.kni.buffers.allocateBuffer
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.longs.shouldBeLessThan
import java.lang.management.BufferPoolMXBean
import java.lang.management.ManagementFactory

private fun directMemoryUsed() = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean::class.java)
    .first { it.name == "direct" }.memoryUsed

private fun gc() = repeat(3) { System.gc(); Thread.sleep(100) }

val BufferTest by testSuite {
    bufferLifetimeTests()

    test("buffer parameters do not leak") {
        gc()
        val before = directMemoryUsed()
        repeat(200) {
            BufferKeeper.nullableBuffer(allocateBuffer(1024 * 1024))
        }
        gc()
        // 200MB would stay reachable if each buffer parameter was pinned by a global ref.
        (directMemoryUsed() - before) shouldBeLessThan 50L * 1024 * 1024
    }
}
