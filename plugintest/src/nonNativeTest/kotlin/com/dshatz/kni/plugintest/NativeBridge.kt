package com.dshatz.kni.plugintest

external fun nativeHello(): String

fun receiveHelloFromNative(): String = nativeHello()