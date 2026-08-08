package com.dshatz.kni.flows

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.resume

open class SuspendCallbackImpl<T>(private val continuation: CancellableContinuation<T>): SuspendCallback<T> {
    override fun onSuccess(value: T) {
        continuation.resume(value)
    }

    override fun onFailure(message: String, stackTrace: String) {
        continuation.cancel(
            CancellationException("Native coroutine was cancelled: $message",
            RuntimeException(stackTrace))
        )
    }

    override fun close() {
    }

}

open class SuspendCallbackImpl0(private val continuation: CancellableContinuation<Unit>): SuspendCallback0 {
    override fun onSuccess() {
        continuation.resume(Unit)
    }

    override fun onFailure(message: String, stackTrace: String) {
        continuation.cancel(
            CancellationException("Native coroutine was cancelled: $message",
            RuntimeException(stackTrace))
        )
    }

    override fun close() {
    }

}