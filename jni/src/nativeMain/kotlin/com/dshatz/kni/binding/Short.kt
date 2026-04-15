package com.dshatz.kni.binding

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ShortVarOf
import platform.posix.int16_t

typealias jshort = int16_t

@OptIn(ExperimentalForeignApi::class)
typealias jshortVar = ShortVarOf<jshort>

@OptIn(ExperimentalForeignApi::class)
typealias jshortArray = jarray