package com.dshatz.kni.serialization.exception

import com.dshatz.kni.serialization.parseNativeStack

actual fun makeException(message: String, stacktrace: String): Exception {
    return RuntimeException(message).also {
        it.stackTrace = parseNativeStack(stacktrace.lines())
    }
}