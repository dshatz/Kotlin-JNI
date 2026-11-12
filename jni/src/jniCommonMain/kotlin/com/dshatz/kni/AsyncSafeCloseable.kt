package com.dshatz.kni

import com.dshatz.kni.annotations.KniInternalApi
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

interface Acquiring: AutoCloseable {
    /**
     * @return false if already closed, in which case nothing was acquired.
     */
    @KniInternalApi
    fun tryAcquire(): Boolean
    @KniInternalApi
    fun release()
}

@OptIn(ExperimentalAtomicApi::class)
abstract class AsyncSafeCloseable : Acquiring {

    private val state = AtomicInt(0)

    @KniInternalApi
    override fun tryAcquire(): Boolean {
        while (true) {
            val current = state.load()
            if ((current and CLOSED_BIT) != 0) {
                return false
            }
            if (state.compareAndSet(current, current + 1)) {
                return true
            }
        }
    }

    @KniInternalApi
    override fun release() {
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