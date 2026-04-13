package kni.test

import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.annotations.CallableFromNative

@CallableFromNative
interface JvmCaller: AutoCloseable {
    fun giveANumber(): Int
    fun fillBuffer(buffer: ByteBuffer): String

    fun withTypeAlias(alias: TestAlias): TestAlias
}