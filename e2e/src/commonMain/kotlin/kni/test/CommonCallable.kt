package kni.test

import com.dshatz.kni.annotations.Callable
import com.dshatz.kni.buffers.ByteBuffer
import com.dshatz.kni.buffers.allocateBuffer

expect object CommonCallable {

    @Callable
    fun makeOrange(value: ColorfulObject): ColorfulObject

    @Callable
    fun fillBuffer(buffer: ByteBuffer): String

}

@Callable
expect fun topLevelFun(): ByteBuffer