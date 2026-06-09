package kni.test

import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.annotations.JniCallback

@JniCallback
interface JvmCaller: AutoCloseable {
    fun giveANumber(): Int
    fun fillBuffer(buffer: ByteBuffer): String

    fun withTypeAlias(alias: TestAlias): TestAlias

    /**
     * Example with nullable type
     */
    fun setError(error: String?)
    fun getError(): String?
}