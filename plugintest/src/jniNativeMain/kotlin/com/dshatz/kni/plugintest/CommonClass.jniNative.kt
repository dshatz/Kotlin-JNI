package com.dshatz.kni.plugintest

import com.dshatz.kni.annotations.JniCall

actual object CommonHello {
    @JniCall
    actual fun nativeHello(): String {
        return "Hello from Native"
    }
}