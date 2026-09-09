package kni

import com.dshatz.kni.annotations.JniSerializable
import com.dshatz.kni.annotations.JniAdapter

@JniAdapter(BitmapAdapter::class)
expect class CommonExternalBitmap {
    val width: Int
    val height: Int
    val strideBytes: Int

    fun erase(color: UInt)
}

expect object BitmapAdapter: com.dshatz.kni.wrapper.JniAdapter<CommonExternalBitmap>

@JniSerializable
data class SkBitmapInfo(
    val ptr: Long,
    val width: Int,
    val height: Int,
    val strideBytes: Int,
)