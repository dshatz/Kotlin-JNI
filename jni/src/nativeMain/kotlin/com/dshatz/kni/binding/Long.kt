package com.dshatz.kni.binding

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.LongVarOf
import platform.posix.int64_t

typealias jlong = int64_t

@OptIn(ExperimentalForeignApi::class)
typealias jlongVar = LongVarOf<jlong>

@OptIn(ExperimentalForeignApi::class)
typealias jlongArray = jarray