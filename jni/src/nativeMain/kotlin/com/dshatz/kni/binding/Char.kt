package com.dshatz.kni.binding

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UShortVarOf
import platform.posix.uint16_t

typealias jchar = uint16_t

@OptIn(ExperimentalForeignApi::class)
typealias jcharVar = UShortVarOf<jchar>

@OptIn(ExperimentalForeignApi::class)
typealias jcharArray = jarray