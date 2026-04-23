package com.dshatz.kni.sample

import com.dshatz.kni.utils.memcpy
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlin.random.Random

actual object Demo {
    @com.dshatz.kni.annotations.JniCall
    actual fun stringExample(): String {
        return "Hello Native!"
    }

    @com.dshatz.kni.annotations.JniCall
    actual fun mixed(
        a: String,
        b: Int,
        c: Boolean,
        d: IntArray,
        e: Char
    ): String {
        return "$a, $b, $c, ${d.joinToString(separator = "|", prefix = "[", postfix = "]")}, $e"
    }

    @OptIn(ExperimentalForeignApi::class)
    @com.dshatz.kni.annotations.JniCall
    actual fun byteBuffer(buffer: com.dshatz.kni.buffers.ByteBuffer, size: Long): ByteArray {
        val bytes = Random.nextBytes(size.toInt())
        bytes.usePinned {
            memcpy(buffer.address, it.addressOf(0), size.toULong())
        }
        return bytes
    }

    @com.dshatz.kni.annotations.JniCall
    actual fun callJvmFromNative(obj: JvmService): String {
        val sum = obj.sum(100, 200).toString()
        val greeting = obj.concat("Hello", "Jni")
        obj.printHello()
        println("Native read bytes from jvm: ${obj.readBytes().toHexString()}")
        return obj.concat(sum, greeting)
    }

    @com.dshatz.kni.annotations.JniCall
    actual fun writeToJvmBuffer(
        bridge: JvmService,
        buffer: com.dshatz.kni.buffers.ByteBuffer
    ): Int {
        return bridge.readBytesTo(buffer)
    }
}