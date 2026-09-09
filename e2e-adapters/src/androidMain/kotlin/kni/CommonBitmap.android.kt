package kni

import android.graphics.Bitmap
import com.dshatz.kni.annotations.JniAdapter
import com.dshatz.kni.wrapper.JvmJniAdapter

@JniAdapter(adapter = BitmapAdapter::class)
actual data class CommonExternalBitmap(
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
    JvmJniAdapter<CommonExternalBitmap, Bitmap> {
    override fun getJniValue(value: CommonExternalBitmap): Bitmap {
        return value.bitmap
    }

    override fun fromJniValue(value: Bitmap): CommonExternalBitmap {
        return CommonExternalBitmap(value)
    }
}