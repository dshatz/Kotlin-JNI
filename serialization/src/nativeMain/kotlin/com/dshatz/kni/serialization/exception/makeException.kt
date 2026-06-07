package com.dshatz.kni.serialization.exception

actual fun makeException(message: String, stacktrace: String): Exception {
    return RuntimeException(message + "\n" + stacktrace)
}