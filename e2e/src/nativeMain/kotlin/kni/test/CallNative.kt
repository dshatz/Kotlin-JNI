package kni.test

import dev.datlag.nkommons.binding.ByteBuffer
import dev.datlag.nkommons.JNIConnect

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