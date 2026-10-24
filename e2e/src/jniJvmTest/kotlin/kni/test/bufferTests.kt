package kni.test

import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.buffers.allocateBuffer
import com.dshatz.kni.buffers.toCommonByteBuffer
import io.kotest.assertions.throwables.shouldThrow
import com.dshatz.kni.load.BundledLibLoader
import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

/**
 * Poll [check] until it returns true or a timeout passes.
 *
 * Suspend calls end the borrow of their buffers after the result has been delivered, so the JVM may resume slightly earlier.
 */
suspend fun eventually(check: () -> Boolean): Boolean {
    repeat(100) {
        if (check()) return true
        delay(20)
    }
    return check()
}

private class TestBufferProvider(private val value: Byte) : BufferProvider {
    private fun buffer(): ByteBuffer = allocateBuffer(16).apply { write(byteArrayOf(value)) }

    override fun provide(): ByteBuffer = buffer()

    override suspend fun provideSuspend(): ByteBuffer {
        delay(50)
        return buffer()
    }

    override fun close() {}
}

fun TestSuiteScope.bufferLifetimeTests() {
    BundledLibLoader.loadBundledLibrary("e2e")

    test("buffer parameter is invalidated after the call") {
        BufferKeeper.storeBorrowed(allocateBuffer(16))
        BufferKeeper.isStoredInvalidated() shouldBe true
    }

    test("retained buffer stays valid until released") {
        val buffer = allocateBuffer(16)
        BufferKeeper.storeRetained(buffer)
        BufferKeeper.writeToRetained(42)
        buffer.readToByteArray(0, 1)[0] shouldBe 42.toByte()
        BufferKeeper.releaseRetained()
    }

    test("nullable buffer parameter") {
        BufferKeeper.nullableBuffer(allocateBuffer(16)) shouldBe 16L
        BufferKeeper.nullableBuffer(null) shouldBe -1L
    }

    test("suspend buffer parameter is usable for the whole call") {
        val buffer = allocateBuffer(16)
        BufferKeeper.fillAfterDelay(buffer, 7, delayMillis = 100)
        buffer.readToByteArray(0, 16).toList() shouldBe List(16) { 7.toByte() }
    }

    test("suspend buffer parameter is invalidated after the call") {
        BufferKeeper.storeBorrowedSuspend(allocateBuffer(16))
        eventually { BufferKeeper.isStoredInvalidated() } shouldBe true
    }

    test("suspend retained buffer stays valid until released") {
        val buffer = allocateBuffer(16)
        BufferKeeper.storeRetainedSuspend(buffer)
        BufferKeeper.writeToRetained(42)
        buffer.readToByteArray(0, 1)[0] shouldBe 42.toByte()
        BufferKeeper.releaseRetained()
    }

    test("nullable suspend buffer parameter") {
        BufferKeeper.nullableBufferSuspend(allocateBuffer(16)) shouldBe 16L
        BufferKeeper.nullableBufferSuspend(null) shouldBe -1L
    }

    test("buffer returned from callback is usable on native") {
        BufferKeeper.readFromProvider(TestBufferProvider(5)) shouldBe 5.toByte()
    }

    test("buffer returned from suspend callback is usable on native") {
        BufferKeeper.readFromSuspendProvider(TestBufferProvider(6)) shouldBe 6.toByte()
    }

    test("non-direct buffer parameter is rejected") {
        val heapBuffer = java.nio.ByteBuffer.allocate(16).toCommonByteBuffer()
        shouldThrow<IllegalArgumentException> {
            BufferKeeper.nullableBuffer(heapBuffer)
        }
    }
}
