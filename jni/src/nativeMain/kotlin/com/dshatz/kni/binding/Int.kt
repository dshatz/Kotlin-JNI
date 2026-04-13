package com.dshatz.kni.binding

import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVarOf
import platform.posix.int32_t

typealias jint = int32_t

@OptIn(ExperimentalForeignApi::class)
typealias jintVar = IntVarOf<jint>

typealias jsize = jint

@OptIn(ExperimentalForeignApi::class)
typealias jintArray = jarray

@OptIn(ExperimentalForeignApi::class)
typealias jintArrayVar = CPointerVarOf<jintArray>