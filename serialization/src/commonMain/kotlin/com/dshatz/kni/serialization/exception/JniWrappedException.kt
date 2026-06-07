package com.dshatz.kni.serialization.exception

class JniWrappedException(
    message: String,
    val nativeStackTrace: String
): Exception(message, makeException(message, nativeStackTrace))