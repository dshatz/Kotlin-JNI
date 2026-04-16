package kni.test

import com.dshatz.kni.annotations.Callable
import com.dshatz.kni.buffers.ByteBuffer


expect object CallerBridge {
    @Callable fun init(caller: JvmCaller)
    @Callable fun dispose()
    @Callable fun askJvmForANumber(): Int
    @Callable fun askJvmToFillBuffer(buffer: ByteBuffer): String
    @Callable fun sendTypeAlias(alias: TestAlias): TestAlias
    @Callable fun callbackFromCoroutine(callback: Callback, coroutineName: String)
}