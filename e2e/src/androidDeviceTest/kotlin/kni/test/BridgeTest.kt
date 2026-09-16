package kni.test

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorSpace
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import kni.CommonExternalBitmap

private fun createBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
    return bitmap
}

val BridgeTest by testSuite {
    bridgeTests()

    test("adapter") {
        val bitmap = createBitmap()
        val common = CommonBitmap(bitmap)
        val worker = BitmapWorker(common)
        val width = worker.eraseBitmap(common)
        width shouldBe 100
        bitmap.getColor(0, 0).toArgb() shouldBe Color.RED
    }

    test("return via adapter") {
        val bitmap = createBitmap()
        val common = CommonBitmap(bitmap)

        val worker = BitmapWorker(common)
        val same = worker.returnBitmap(common)
        same.bitmap shouldBe bitmap
    }

    test("return via external adapter") {
        val bitmap = createBitmap()
        val commonExternal = CommonExternalBitmap(bitmap)
        val common = CommonBitmap(bitmap)

        val worker = BitmapWorker(common)
        val same = worker.returnExternalBitmap(commonExternal)
        same.bitmap shouldBe bitmap
    }
}