package com.dshatz.kni.serialization.exception

actual fun makeException(message: String, stacktrace: String): kotlin.Exception {
    return RuntimeException(message + "\n" + stacktrace)
}