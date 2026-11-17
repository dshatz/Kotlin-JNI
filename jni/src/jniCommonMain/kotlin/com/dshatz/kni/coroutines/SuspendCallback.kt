package com.dshatz.kni.coroutines

/**
 * Base callback type for wrapping suspend function calls across JNI boundary.
 */
interface SuspendCallback<T>: AutoCloseable {
    fun onSuccess(value: T)
    fun onFailure(message: String, stackTrace: String)
}

