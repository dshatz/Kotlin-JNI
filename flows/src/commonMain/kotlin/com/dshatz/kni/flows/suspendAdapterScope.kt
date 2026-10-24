package com.dshatz.kni.flows

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val suspendAdapterScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

/**
 * Run [block] in [suspendAdapterScope] and report the result to this callback.
 *
 * [onComplete] runs once the coroutine has completed, after the result has been reported. It also runs if the coroutine is cancelled before starting.
 */
fun <T> SuspendCallback<T>.executeSuspend(onComplete: () -> Unit = {}, block: suspend () -> T) {
    suspendAdapterScope.launch {
        try {
            val result = block()
            onSuccess(result)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                onFailure("Cancelled", e.stackTraceToString())
            } else {
                onFailure(e.message.orEmpty(), e.cause.toString() + "\n" + e.stackTraceToString())
            }
        } finally {
            close()
        }
    }.invokeOnCompletion {
        onComplete()
    }
}


/**
 * Run [block] in [suspendAdapterScope] and report completion to this callback.
 *
 * [onComplete] runs once the coroutine has completed, after the result has been reported. It also runs if the coroutine is cancelled before starting.
 */
fun SuspendCallback0.executeSuspend(onComplete: () -> Unit = {}, block: suspend () -> Unit) {
    suspendAdapterScope.launch {
        try {
            block()
            onSuccess()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                onFailure("Cancelled", e.stackTraceToString())
            } else {
                onFailure(e.message.orEmpty(), e.stackTraceToString())
            }
        } finally {
            close()
        }
    }.invokeOnCompletion {
        it?.printStackTrace()
        onComplete()
    }
}
