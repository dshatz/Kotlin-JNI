package com.dshatz.kni.binding

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVarOf
import platform.posix.uint8_t

typealias jboolean = uint8_t

@OptIn(ExperimentalForeignApi::class)
typealias jbooleanVar = UByteVarOf<jboolean>

@OptIn(ExperimentalForeignApi::class)
typealias jbooleanArray = jarray