package com.dshatz.kni

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
abstract class AsyncSafeCloseable : AutoCloseable {

    private val state = AtomicInt(0)

    @PublishedApi
    internal fun acquire() {
        while (true) {
            val current = state.load()
            if ((current and CLOSED_BIT) != 0) {
                error("${this::class.simpleName} is closed.")
            }
            if (state.compareAndSet(current, current + 1)) {
                return
            }
        }
    }

    @PublishedApi
    internal fun release() {
        while (true) {
            val current = state.load()
            val next = current - 1
            if (state.compareAndSet(current, next)) {
                if ((next and COUNTER_MASK) == 0 && (next and CLOSED_BIT) != 0) {
                    performCleanup()
                }
                return
            }
        }
    }

    override fun close() {
        while (true) {
            val current = state.load()
            if ((current and CLOSED_BIT) != 0) return

            val next = current or CLOSED_BIT
            if (state.compareAndSet(current, next)) {
                if ((next and COUNTER_MASK) == 0) {
                    performCleanup()
                }
                break
            }
        }
    }

    protected abstract fun performCleanup()

    companion object {
        @PublishedApi
        internal const val CLOSED_BIT: Int = 1 shl 31

        @PublishedApi
        internal const val COUNTER_MASK: Int = 0x7FFFFFFF
    }
}