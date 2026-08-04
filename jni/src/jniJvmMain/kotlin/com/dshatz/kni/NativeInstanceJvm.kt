package com.dshatz.kni

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class NativeInstanceJvm: AutoCloseable {
    @Volatile
    public var nativeInstance: Long = 0L
        protected set

    @Volatile
    protected var isClosed: Boolean = false

    protected val instanceLock = ReentrantReadWriteLock()

    protected inline fun <T> withValidInstance(block: (Long) -> T): T {
        return instanceLock.read {
            val handle = nativeInstance
            if (isClosed || handle == 0L) {
                error("${this::class.simpleName} is closed.")
            }
            block(handle)
        }
    }

    public abstract fun disposeNative(instance: Long)

    override fun close() {
        instanceLock.write {
            if (isClosed) return
            isClosed = true
            val handleToDispose = nativeInstance
            nativeInstance = 0L
            if (handleToDispose != 0L) {
                disposeNative(handleToDispose)
            }
        }
    }
}