package com.dshatz.kni

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

interface NativeInstanceJvm : Acquiring {
    @OptIn(ExperimentalAtomicApi::class)
    val nativeInstance: AtomicLong
    fun asLong(): Long
}
