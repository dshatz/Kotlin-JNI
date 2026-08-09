package com.dshatz.kni.flows

import kotlinx.coroutines.flow.MutableStateFlow

actual class NativeBackedFlow<T> actual constructor(defaultValue: T) {
    actual val flow: kotlinx.coroutines.flow.StateFlow<T> = MutableStateFlow(defaultValue)
}
