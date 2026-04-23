package com.dshatz.kni.sample

import com.dshatz.kni.annotations.JniCallback

@JniCallback
interface JvmCallback: AutoCloseable {
    fun sayHello(): String
}