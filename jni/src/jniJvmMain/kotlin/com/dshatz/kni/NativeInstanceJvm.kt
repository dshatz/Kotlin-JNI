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
abstract class NativeInstanceJvm() : AutoCloseable {

    constructor(nativeInstancePtr: Long) : this() {
        nativeInstance.store(nativeInstancePtr)
    }

    public val nativeInstance: AtomicLong = AtomicLong(0L)

    protected val nativeInstancePtr get() = nativeInstance.load()

    // Tracks active operations to prevent close() from freeing memory early
    val activeOperations = AtomicInt(0)

    @Volatile
    protected var isClosed: Boolean = false

    protected inline fun <T> withValidInstance(block: (Long) -> T): T {
        activeOperations.incrementAndFetch()
        try {
            if (isClosed) {
                error("${this::class.simpleName} is closed.")
            }
            val handle = nativeInstance.load()
            if (handle == 0L) {
                error("${this::class.simpleName} is closed.")
            }
            return block(handle)
        } finally {
            activeOperations.decrementAndFetch()
        }
    }

    protected suspend inline fun <T> withValidInstanceSuspend(crossinline block: suspend (Long) -> T): T {
        activeOperations.incrementAndFetch()
        try {
            if (isClosed) {
                error("${this::class.simpleName} is closed.")
            }
            val handle = nativeInstance.load()
            if (handle == 0L) {
                error("${this::class.simpleName} is closed.")
            }
            return block(handle)
        } finally {
            activeOperations.decrementAndFetch()
        }
    }

    public abstract fun disposeNative(instance: Long)

    override fun close() {
        if (isClosed) return

        // Wait safely until all active sync and async operations finish
        while (activeOperations.load() > 0) {
            Thread.yield()
        }
        isClosed = true

        val handleToDispose = nativeInstance.exchange(0L)
        if (handleToDispose != 0L) {
            disposeNative(handleToDispose)
        }
    }
}