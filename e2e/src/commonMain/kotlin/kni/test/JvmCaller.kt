package kni.test

import dev.datlag.nkommons.binding.ByteBuffer
import dev.datlag.nkommons.CallableFromNative
import dev.datlag.nkommons.Disposable

@CallableFromNative
interface JvmCaller: Disposable {
    fun giveANumber(): Int
    fun fillBuffer(buffer: ByteBuffer): String

    fun withTypeAlias(alias: TestAlias): TestAlias
}