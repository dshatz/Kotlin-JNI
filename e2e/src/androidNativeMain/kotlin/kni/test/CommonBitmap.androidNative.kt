package kni.test

import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.JavaVMVar
import com.dshatz.kni.annotations.JniAdapter
import com.dshatz.kni.binding.jobject
import com.dshatz.kni.utils.WithAttachedThread
import com.dshatz.kni.utils.getJavaVM
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.value
import platform.android.AndroidBitmapInfo
import platform.android.AndroidBitmap_getInfo
import platform.android.AndroidBitmap_lockPixels
import platform.android.AndroidBitmap_unlockPixels

@OptIn(ExperimentalForeignApi::class)
@JniAdapter(adapter = BitmapAdapter::class)
actual data class CommonBitmap(
    val bitmap: jobject,
    actual val width: Int,
    actual val height: Int,
    actual val strideBytes: Int,
    val jvm: CPointer<JavaVMVar>
) {
    fun useLocked(block: (address: CPointer<*>) -> Unit) {
        jvm.WithAttachedThread { env ->
            memScoped {
                val pixelsPtrVar = alloc<COpaquePointerVar>()
                AndroidBitmap_lockPixels(env, bitmap, pixelsPtrVar.ptr)
                try {
                    block(pixelsPtrVar.value!!)
                } finally {
                    AndroidBitmap_unlockPixels(env, bitmap)
                }
            }
        }
    }

    fun swapRedAndBlue(color: UInt): UInt {
        val aAndG = color and 0xFF00FF00u // Keeps Alpha (bits 24-31) and Green (bits 8-15)
        val r = (color shr 16) and 0xFFu
        val b = color and 0xFFu

        return aAndG or (b shl 16) or r
    }

    actual fun erase(color: UInt) {
        val swapped = swapRedAndBlue(color)
        useLocked { addr ->
            val uintPtr = addr.reinterpret<UIntVar>()
            val totalPixels = width * height

            for (i in 0 until totalPixels) {
                uintPtr[i] = swapped
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual object BitmapAdapter :
    com.dshatz.kni.wrapper.NativeJniAdapter<CommonBitmap, jobject> {
    override fun fromJni(
        env: CPointer<JNIEnvVar>,
        value: jobject
    ): CommonBitmap {
        return memScoped {
            val bitmapInfo = alloc<AndroidBitmapInfo>()
            AndroidBitmap_getInfo(env, value, bitmapInfo.ptr)
            CommonBitmap(
                value,
                bitmapInfo.width.convert(),
                bitmapInfo.height.convert(),
                bitmapInfo.stride.convert<Int>(),
                env.getJavaVM()
            )
        }
    }

    override fun toJni(
        env: CPointer<JNIEnvVar>,
        value: CommonBitmap
    ): jobject {
        return value.bitmap
    }
}