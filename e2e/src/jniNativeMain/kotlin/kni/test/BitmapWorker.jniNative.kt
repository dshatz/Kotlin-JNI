package kni.test

import com.dshatz.kni.annotations.JniCall
import kni.CommonExternalBitmap

actual class BitmapWorker actual constructor(bitmap: CommonBitmap) : AutoCloseable {
    @JniCall
    actual fun eraseBitmap(bitmap: CommonBitmap): Int {
        bitmap.erase(0xFFFF0000u)
        return bitmap.width
    }

    @JniCall
    actual fun returnBitmap(bitmap: CommonBitmap): CommonBitmap {
        return bitmap
    }

    actual override fun close() {
    }

    @JniCall
    actual fun returnExternalBitmap(bitmap: CommonExternalBitmap): CommonExternalBitmap {
        return bitmap
    }
}