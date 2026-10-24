package kni.test

import com.dshatz.kni.buffers.allocateBuffer
import com.dshatz.kni.load.BundledLibLoader
import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.matchers.shouldBe

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
}
