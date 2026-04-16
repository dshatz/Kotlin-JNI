package kni.test

import com.dshatz.kni.annotations.Callable
import com.dshatz.kni.buffers.ByteBuffer
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual object CallerBridge {

    lateinit var callerRef: JvmCaller

    @Callable
    actual fun init(caller: JvmCaller) {
        callerRef = caller
    }

    @Callable
    actual fun dispose() {
        callerRef.close()
    }

    @Callable
    actual fun askJvmForANumber(): Int {
        return callerRef.giveANumber()
    }

    @Callable
    actual fun askJvmToFillBuffer(buffer: ByteBuffer): String {
        return callerRef.fillBuffer(buffer)
    }

    @Callable
    actual fun sendTypeAlias(alias: TestAlias): TestAlias {
        return callerRef.withTypeAlias(alias)
    }

    val bridgeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Callable
    actual fun callbackFromCoroutine(callback: Callback, coroutineName: String) {
        bridgeScope.launch(CoroutineName(coroutineName)) {
            callback.onComplete(true)
        }
    }
}