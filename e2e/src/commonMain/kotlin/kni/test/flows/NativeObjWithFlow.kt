package kni.test.flows

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.flows.NativeBackedFlow

expect class NativeObjWithFlow(): AutoCloseable {

    @JniCall
    fun increment(by: Int)

    @JniCall
    fun doubleAndGet(): Int

    val myFlow: NativeBackedFlow<Int>

    override fun close()
}