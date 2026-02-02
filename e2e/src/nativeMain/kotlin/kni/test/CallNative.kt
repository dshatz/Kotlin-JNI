package kni.test

import dev.datlag.nkommons.ByteBuffer
import dev.datlag.nkommons.JNIClassName
import dev.datlag.nkommons.JNIConnect
import dev.datlag.nkommons.JNIPackageName

@JNIConnect
@JNIPackageName("kni.test")
@JNIClassName("CallTestKt")
fun askJvmForANumber(caller: JvmCaller): Int {
    return caller.giveANumber()
}

@JNIConnect
@JNIPackageName("kni.test")
@JNIClassName("CallTestKt")
fun askJvmToFillBuffer(caller: JvmCaller, buffer: ByteBuffer): String {
    return caller.fillBuffer(buffer)
}