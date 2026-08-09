package com.dshatz.kni.flows

import kotlinx.coroutines.flow.StateFlow

expect class NativeBackedFlow<T>(defaultValue: T) {
    val flow: StateFlow<T>
}


interface FlowCallback<T>: AutoCloseable {
    fun onValue(value: T)
}
