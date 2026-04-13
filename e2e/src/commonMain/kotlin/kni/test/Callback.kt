package kni.test

import com.dshatz.kni.annotations.CallableFromNative

@CallableFromNative
interface Callback: AutoCloseable {
    fun onComplete(result: Boolean)
}