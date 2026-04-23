package kni.test

import com.dshatz.kni.annotations.JniCallback

@JniCallback
interface Callback: AutoCloseable {
    fun onComplete(result: Boolean)
}