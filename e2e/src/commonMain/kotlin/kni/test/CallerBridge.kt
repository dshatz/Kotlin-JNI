package kni.test

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.buffers.ByteBuffer


expect object CallerBridge {
    @JniCall fun init(caller: JvmCaller)
    @JniCall fun dispose()
    @JniCall fun askJvmForANumber(): Int
    @JniCall fun askJvmToFillBuffer(buffer: ByteBuffer): String
    @JniCall fun sendTypeAlias(alias: TestAlias): TestAlias
    @JniCall fun callbackFromCoroutine(callback: Callback, coroutineName: String)
    @JniCall fun callbackWithData(callback: Callback, data: ColorfulObject)
    @JniCall fun giveResult(code: Int, error: Boolean): Result<Int>
}