package kni.test

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

private fun createBitmap(): Bitmap {
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(ColorInfo(ColorType.BGRA_8888, ColorAlphaType.PREMUL, ColorSpace.sRGB), 100, 200))
    return bitmap
}

val BridgeTest by testSuite {
    bridgeTests()

    test("adapter") {
        val bitmap = createBitmap()
        val common = CommonBitmap(bitmap.peekPixels()!!)
        val worker = BitmapWorker(common)
        val width = worker.eraseBitmap(common)
        width shouldBe 100
        bitmap.getColor(0, 0).toUInt() shouldBe 0xFFFF0000u
    }

    test("return via adapter") {
        val bitmap = createBitmap()
        val pixmap = bitmap.peekPixels()!!
        val common = CommonBitmap(pixmap)

        val worker = BitmapWorker(common)
        val same = worker.returnBitmap(common)
        same.pixmap.addr shouldBe pixmap.addr
    }
}