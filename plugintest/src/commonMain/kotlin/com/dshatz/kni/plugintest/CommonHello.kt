package com.dshatz.kni.plugintest

import com.dshatz.kni.annotations.Callable

expect object CommonHello {
    @Callable fun nativeHello(): String
}