package com.dshatz.kni.flows

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

actual class NativeBackedFlow<T> actual constructor(defaultValue: T) {

    private val mutableFlow: MutableStateFlow<T> = MutableStateFlow(defaultValue)
    actual val flow: StateFlow<T> = mutableFlow.asStateFlow()

    private var jvmCallback: FlowCallback<T>? = null

    var value: T
        get() = mutableFlow.value
        set(value) {
            mutableFlow.value = value
            jvmCallback?.onValue(value)
        }

    fun updateAndGet(action: (T) -> T): T {
        val newValue = mutableFlow.updateAndGet(action)
        jvmCallback?.onValue(newValue)
        return newValue
    }

    fun update(action: (T) -> T) {
        updateAndGet(action)
    }

    fun compareAndSet(expect: T, update: T): Boolean {
        val changed = mutableFlow.compareAndSet(expect, update)
        if (changed) jvmCallback?.onValue(update)
        return changed
    }


    fun bindToJvm(callback: FlowCallback<T>): T {
        jvmCallback = callback
        return value
    }
}