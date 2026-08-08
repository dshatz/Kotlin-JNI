package com.dshatz.kni.flows

interface SuspendCallback0: AutoCloseable {
    fun onSuccess()
    fun onFailure(message: String, stackTrace: String)
}