package com.dshatz.kni

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

class CoroutineReadWriteMutex {
    private val writeMutex = Mutex()
    private val stateMutex = Mutex()
    private val readerCount = AtomicInteger(0)

    suspend fun <T> withReadLock(block: suspend () -> T): T {
        stateMutex.withLock {
            val count = readerCount.incrementAndGet()
            if (count == 1) {
                // First reader locks out writers
                writeMutex.lock()
            }
        }
        try {
            return block()
        } finally {
            stateMutex.withLock {
                val count = readerCount.decrementAndGet()
                if (count == 0) {
                    // Last reader unlocks writers
                    writeMutex.unlock()
                }
            }
        }
    }

    suspend fun <T> withWriteLock(block: suspend () -> T): T {
        writeMutex.withLock {
            return block()
        }
    }
}