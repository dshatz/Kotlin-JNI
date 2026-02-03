package kni.test

import dev.datlag.nkommons.CallableFromNative
import dev.datlag.nkommons.binding.ByteBuffer

@CallableFromNative
interface JvmCaller: AutoCloseable {
    fun giveANumber(): Int
    fun fillBuffer(buffer: ByteBuffer): String

    fun withTypeAlias(alias: TestAlias): TestAlias
}