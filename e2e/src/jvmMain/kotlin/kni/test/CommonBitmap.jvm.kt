package kni.test

import com.dshatz.kni.annotations.JniAdapter
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Pixmap
import org.jetbrains.skia.impl.Managed

@JniAdapter(adapter = BitmapAdapter::class)
actual data class CommonBitmap(
    val pixmap: Pixmap
) {
    actual val width: Int = pixmap.info.width
    actual val height: Int = pixmap.info.height
    actual val strideBytes: Int = pixmap.rowBytes

    fun asImage(): Image {
        return Image.makeFromPixmap(pixmap)
    }

    actual fun erase(color: UInt) {
        pixmap.erase(color.toInt())
    }
}

actual object BitmapAdapter :
    com.dshatz.kni.wrapper.JvmJniAdapter<CommonBitmap, SkBitmapInfo> {
    override fun getJniValue(value: CommonBitmap): SkBitmapInfo {
        val info = value.pixmap.info
        return SkBitmapInfo(
            value.pixmap.addr,
            info.width,
            info.height,
            value.pixmap.rowBytes
        )
    }

    override fun fromJniValue(value: SkBitmapInfo): CommonBitmap {
        val data = Data.makeWithoutCopy(
            value.ptr,
            length = value.strideBytes * value.height,
            object: Managed(value.ptr, 0, false) {}
        )
        val pixmap = Pixmap.make(
            ImageInfo(
                ColorInfo(ColorType.BGRA_8888, ColorAlphaType.PREMUL, null),
                value.height,
                value.width
            ),
            data,
            value.strideBytes
        )
        return CommonBitmap(pixmap)
    }
}