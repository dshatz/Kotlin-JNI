package com.dshatz.kni.plugintest

import com.dshatz.kni.annotations.JNIConnect

@JNIConnect(
    packageName = "com.dshatz.kni.plugintest",
    className = "NativeBridgeKt"
)
fun nativeHello(): String {
    return "Hello from Native"
}