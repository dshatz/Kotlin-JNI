package com.dshatz.kni.plugintest

import com.dshatz.kni.annotations.JniCall

expect object CommonHello {
    @JniCall fun nativeHello(): String
}