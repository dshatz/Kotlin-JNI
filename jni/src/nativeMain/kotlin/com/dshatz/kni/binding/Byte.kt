package com.dshatz.kni.binding

import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.int8_t

typealias jbyte = int8_t

@OptIn(ExperimentalForeignApi::class)
typealias jbyteVar = ByteVarOf<jbyte>

@OptIn(ExperimentalForeignApi::class)
typealias jbyteArray = jarray