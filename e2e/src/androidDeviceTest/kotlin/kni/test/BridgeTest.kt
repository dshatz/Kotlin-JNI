package kni.test

import android.graphics.Bitmap
import android.graphics.Color
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe


val BridgeTest by testSuite {
    bridgeTests()

    test("adapter") {
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val width = CommonCallable.eraseBitmap(CommonBitmap(bitmap))
        width shouldBe 200
        bitmap.getColor(0, 0).toArgb() shouldBe Color.RED
    }
}