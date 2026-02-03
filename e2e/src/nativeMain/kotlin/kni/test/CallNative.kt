package kni.test

import dev.datlag.nkommons.binding.ByteBuffer
import dev.datlag.nkommons.JNIConnect

@JNIConnect(
    packageName = "kni.test",
    className = "CallTestKt"
)
fun askJvmForANumber(caller: JvmCaller): Int {
    return caller.giveANumber()
}

@JNIConnect(
    packageName = "kni.test",
    className = "CallTestKt"
)
fun askJvmToFillBuffer(caller: JvmCaller, buffer: ByteBuffer): String {
    return caller.fillBuffer(buffer)
}