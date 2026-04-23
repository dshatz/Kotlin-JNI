package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.buffers.ByteBuffer

expect object CommonCallable {

    @JniCall
    fun makeOrange(value: ColorfulObject): ColorfulObject

    @JniCall
    fun fillBuffer(buffer: ByteBuffer): String

}

@JniCall
expect fun topLevelFun(): ByteBuffer