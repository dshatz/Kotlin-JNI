package com.dshatz.kni.serialization

actual fun makeException(message: String, stacktrace: String): Exception {
    return RuntimeException(message).also {
        it.stackTrace = parseNativeStack(stacktrace.lines())
    }
}