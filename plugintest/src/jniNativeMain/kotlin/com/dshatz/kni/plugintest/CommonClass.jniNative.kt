package com.dshatz.kni.plugintest

import com.dshatz.kni.annotations.Callable

actual object CommonHello {
    @Callable
    actual fun nativeHello(): String {
        return "Hello from Native"
    }
}