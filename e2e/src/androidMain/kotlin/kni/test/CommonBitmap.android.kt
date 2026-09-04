package kni.test

import android.graphics.Bitmap
import com.dshatz.kni.annotations.JniAdapter

@JniAdapter(adapter = BitmapAdapter::class)
actual data class CommonBitmap(
    val bitmap: Bitmap
) {
    actual val width: Int = bitmap.width
    actual val height: Int = bitmap.height
    actual val strideBytes: Int = bitmap.rowBytes

    actual fun erase(color: UInt) {
        bitmap.eraseColor(color.toInt())
    }
}

actual object BitmapAdapter :
    com.dshatz.kni.wrapper.JvmJniAdapter<CommonBitmap, Bitmap> {
    override fun getJniValue(value: CommonBitmap): Bitmap {
        return value.bitmap
    }

    override fun fromJniValue(value: Bitmap): CommonBitmap {
        return CommonBitmap(value)
    }
}