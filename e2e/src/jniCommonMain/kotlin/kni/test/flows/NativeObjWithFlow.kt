package kni.test.flows

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.flows.NativeBackedFlow
import kni.test.serializable.ColorfulObject

expect class NativeObjWithFlow(): AutoCloseable {

    @JniCall
    fun increment(by: Int)

    @JniCall
    fun doubleAndGet(): Int

    val myFlow: NativeBackedFlow<Int>
    val objectFlow: NativeBackedFlow<ColorfulObject?>

    override fun close()
}