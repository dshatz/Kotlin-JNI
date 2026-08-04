package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.buffers.ByteBuffer
import kni.test.serializable.ColorfulObject
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual object CallerBridge {

    lateinit var callerRef: JvmCaller

    @JniCall
    actual fun init(caller: JvmCaller) {
        callerRef = caller
    }

    @JniCall
    actual fun dispose() {
        callerRef.close()
    }

    @JniCall
    actual fun askJvmForANumber(): Int {
        return callerRef.giveANumber()
    }

    @JniCall
    actual fun askJvmToFillBuffer(buffer: ByteBuffer): String {
        return callerRef.fillBuffer(buffer)
    }

    @JniCall
    actual fun sendTypeAlias(alias: TestAlias): TestAlias {
        return callerRef.withTypeAlias(alias)
    }

    val bridgeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @JniCall
    actual fun callbackFromCoroutine(callback: Callback, coroutineName: String) {
        bridgeScope.launch(CoroutineName(coroutineName)) {
            callback.onComplete(true)
        }
    }

    @JniCall
    actual fun callbackWithData(callback: Callback, data: ColorfulObject) {
        callback.onCompleteWithData(data)
    }

    @JniCall
    actual fun giveResult(code: Int, error: Boolean): Result<Int> {
        return runCatching {
            if (error) error("Native error")
            else code
        }
    }

    @JniCall
    actual fun setError(error: String?) {
        callerRef.setError(error)
    }

    @JniCall
    actual fun getError(): String? {
        return callerRef.getError()
    }
}