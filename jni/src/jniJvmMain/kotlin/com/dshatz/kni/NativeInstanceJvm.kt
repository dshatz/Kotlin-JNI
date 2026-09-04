package com.dshatz.kni

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.concurrent.atomics.update
import kotlin.concurrent.read
import kotlin.concurrent.write

@OptIn(ExperimentalAtomicApi::class)
abstract class NativeInstanceJvm() : AsyncSafeCloseable() {

    constructor(nativeInstancePtr: Long) : this() {
        nativeInstance.store(nativeInstancePtr)
    }

    public val nativeInstance: AtomicLong = AtomicLong(0L)

    protected val nativeInstancePtr get() = nativeInstance.load()

    fun asLong(): Long {
        return nativeInstancePtr
    }

    protected inline fun <T> withValidInstance(block: (Long) -> T): T {
        acquire()
        try {
            val handle = nativeInstance.load()
            if (handle == 0L) {
                error("${this::class.simpleName} is closed.")
            }
            return block(handle)
        } finally {
            release()
        }
    }

    protected suspend inline fun <T> withValidInstanceSuspend(crossinline block: suspend (Long) -> T): T {
        acquire()
        try {
            val handle = nativeInstance.load()
            if (handle == 0L) {
                error("${this::class.simpleName} is closed.")
            }
            return block(handle)
        } finally {
            release()
        }
    }

    public abstract fun disposeNative(instance: Long)

    override fun performCleanup() {
        val handleToDispose = nativeInstance.exchange(0L)
        if (handleToDispose != 0L) {
            disposeNative(handleToDispose)
        }
    }
}