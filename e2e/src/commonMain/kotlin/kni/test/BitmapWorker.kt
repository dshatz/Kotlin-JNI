package kni.test

import com.dshatz.kni.annotations.JniCall

expect class BitmapWorker(
    bitmap: CommonBitmap
): AutoCloseable {
    @JniCall
    fun eraseBitmap(bitmap: CommonBitmap): Int

    @JniCall
    fun returnBitmap(bitmap: CommonBitmap): CommonBitmap
    override fun close()
}