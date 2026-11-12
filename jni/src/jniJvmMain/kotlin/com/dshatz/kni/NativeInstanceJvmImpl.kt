package com.dshatz.kni

import com.dshatz.kni.annotations.KniInternalApi
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Default [NativeInstanceJvm] implementation, meant to be used via delegation by generated classes:
 * `class X : NativeInstanceJvm by NativeInstanceJvmImpl(Companion::disposeNative)`.
 *
 * @param dispose releases the native instance. Each generated class passes its own external function,
 * since calls from inside a delegate never dispatch back to the delegating class.
 */
@OptIn(ExperimentalAtomicApi::class)
class NativeInstanceJvmImpl(private val dispose: (Long) -> Unit) : AsyncSafeCloseable(), NativeInstanceJvm {

    override val nativeInstance: AtomicLong = AtomicLong(0L)

    override fun asLong(): Long = nativeInstance.load()

    override fun performCleanup() {
        val handleToDispose = nativeInstance.exchange(0L)
        if (handleToDispose != 0L) {
            dispose(handleToDispose)
        }
    }
}

@OptIn(KniInternalApi::class, ExperimentalAtomicApi::class)
inline fun <T> NativeInstanceJvm.withValidInstance(block: (Long) -> T): T {
    // Use the receiver's (generated class) name, not the delegate's.
    if (!tryAcquire()) {
        error("${this::class.simpleName} is closed.")
    }
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

@OptIn(KniInternalApi::class, ExperimentalAtomicApi::class)
suspend inline fun <T> NativeInstanceJvm.withValidInstanceSuspend(crossinline block: suspend (Long) -> T): T {
    // Use the receiver's (generated class) name, not the delegate's.
    if (!tryAcquire()) {
        error("${this::class.simpleName} is closed.")
    }
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
