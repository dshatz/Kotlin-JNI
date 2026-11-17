package com.dshatz.kni.coroutines

interface SuspendCallback0: AutoCloseable {
    fun onSuccess()
    fun onFailure(message: String, stackTrace: String)
}