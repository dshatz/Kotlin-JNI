package kni.test

import com.dshatz.kni.annotations.JNIConnect
import com.dshatz.kni.buffers.ByteBuffer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlin.native.concurrent.ObsoleteWorkersApi

lateinit var callerRef: JvmCaller

@JNIConnect(
    packageName = "kni.test",
    className = "CallerBridge"
)
fun askJvmForANumber(): Int {
    return callerRef.giveANumber()
}

@JNIConnect(
    packageName = "kni.test",
    className = "CallerBridge"
)
fun askJvmToFillBuffer(buffer: ByteBuffer): String {
    return callerRef.fillBuffer(buffer)
}

@JNIConnect(
    packageName = "kni.test",
    className = "CallerBridge",
)
fun sendTypeAlias(alias: TestAlias): TestAlias {
    return callerRef.withTypeAlias(alias)
}


@JNIConnect(
    packageName = "kni.test",
    className = "CallerBridge",
)
fun init(caller: JvmCaller) {
    callerRef = caller
}

@JNIConnect(
    packageName = "kni.test",
    className = "CallerBridge",
)
fun dispose() {
    callerRef.close()
}


val bridgeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

@OptIn(ObsoleteWorkersApi::class, ExperimentalForeignApi::class)
@JNIConnect(
    packageName = "kni.test",
    className = "CallerBridge",
)
fun callbackFromCoroutine(callback: Callback, coroutineName: String) {
    bridgeScope.launch(CoroutineName(coroutineName)) {
        callback.onComplete(true)
    }
}