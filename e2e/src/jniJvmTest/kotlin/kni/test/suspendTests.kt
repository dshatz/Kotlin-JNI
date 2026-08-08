package kni.test

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe

val suspendTests by testSuite {
    test("suspend callback") {
        val data = ByteArray(10) {
            it.toByte()
        }
        val callback = object: SuspendingCallback {
            override suspend fun getBlock(): ByteArray {
                return data
            }

            override fun close() {

            }

        }
        val obj = SuspendClass(callback)
        obj.callToSuspendJvm() shouldBe data
    }
}