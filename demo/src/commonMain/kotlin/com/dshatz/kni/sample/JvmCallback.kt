package com.dshatz.kni.sample

import com.dshatz.kni.annotations.CallableFromNative

@CallableFromNative
interface JvmCallback: AutoCloseable {
    fun sayHello(): String
}