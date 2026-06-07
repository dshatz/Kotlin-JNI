package com.dshatz.kni.serialization.exception

expect fun makeException(
    message: String,
    stacktrace: String
): Exception