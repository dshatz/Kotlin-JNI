package kni.test.flows

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.flows.NativeBackedFlow
import kotlinx.coroutines.flow.MutableStateFlow

actual class NativeObjWithFlow actual constructor(): AutoCloseable {
    @JniCall
    actual fun increment(by: Int) {
        myFlow.value += by
    }

    actual val myFlow: NativeBackedFlow<Int> = NativeBackedFlow<Int>(11)
    actual override fun close() {
    }

    @JniCall
    actual fun doubleAndGet(): Int {
        return myFlow.updateAndGet { it * 2 }
    }
}