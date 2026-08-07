package com.dshatz.kni.flows

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val nativeScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

fun <T> SuspendCallback<T>.executeSuspend(block: suspend () -> T) {
    nativeScope.launch {
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
    }
}


fun SuspendCallback0.executeSuspend(block: suspend () -> Unit) {
    nativeScope.launch {
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
    }
}
