package kni.test

import dev.datlag.nkommons.ByteBuffer
import dev.datlag.nkommons.CallableFromNative

@CallableFromNative
interface JvmCaller {
    fun giveANumber(): Int
    fun fillBuffer(buffer: ByteBuffer): String
}