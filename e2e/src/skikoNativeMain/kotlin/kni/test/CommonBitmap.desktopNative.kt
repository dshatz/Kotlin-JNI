package kni.test

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.wrapper.NativeJniAdapter
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toCPointer
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Pixmap
import org.jetbrains.skia.impl.Managed
import org.jetbrains.skia.impl.NativePointer

@com.dshatz.kni.annotations.JniAdapter(adapter = kni.test.BitmapAdapter::class)
actual data class CommonBitmap(
    val pixmap: Pixmap
) {
    actual val width: Int = pixmap.info.width
    actual val height: Int = pixmap.info.height
    actual val strideBytes: Int = pixmap.rowBytes
    actual fun erase(color: UInt) {
        pixmap.erase(color.toInt())
    }
}

@OptIn(ExperimentalForeignApi::class)
actual object BitmapAdapter :
    NativeJniAdapter<CommonBitmap, SkBitmapInfo> {
    override fun fromJni(
        env: CPointer<JNIEnvVar>,
        value: SkBitmapInfo
    ): CommonBitmap {
        val ptr: COpaquePointer = value.ptr.toCPointer()!!
        val data = Data.makeWithoutCopy(
            ptr.rawValue,
            length = value.strideBytes * value.height,
            object: Managed(ptr.rawValue, NativePointer.NULL, false) {}
        )
        val imageInfo = ImageInfo(
            width = value.width,
            height = value.height,
            colorType = ColorType.BGRA_8888,
            alphaType = ColorAlphaType.PREMUL
        )
        val pixmap = Pixmap.make(imageInfo, data, value.strideBytes)
        return CommonBitmap(pixmap)
    }

    override fun toJni(
        env: CPointer<JNIEnvVar>,
        value: CommonBitmap
    ): SkBitmapInfo {
        return SkBitmapInfo(
            value.pixmap.addr.toLong(),
            value.width,
            value.height,
            value.strideBytes
        )
    }

}