package kni.test

import com.dshatz.kni.annotations.JniCall
import kni.CommonExternalBitmap

expect class BitmapWorker(
    bitmap: CommonBitmap,
): AutoCloseable {
    @JniCall
    fun eraseBitmap(bitmap: CommonBitmap): Int

    @JniCall
    fun returnBitmap(bitmap: CommonBitmap): CommonBitmap

    @JniCall
    fun returnExternalBitmap(bitmap: CommonExternalBitmap): CommonExternalBitmap

    override fun close()
}