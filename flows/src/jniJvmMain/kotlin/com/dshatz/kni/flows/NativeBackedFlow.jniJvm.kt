package com.dshatz.kni.flows

import com.dshatz.kni.annotations.KniInternalApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class NativeBackedFlow<T> actual constructor(defaultValue: T) {
    private val mutableFlow = MutableStateFlow<T>(defaultValue)
    actual val flow = mutableFlow.asStateFlow()

    val value: T
        get() = mutableFlow.value

    @KniInternalApi
    fun onValue(value: T) {
        mutableFlow.value = value
    }
}
