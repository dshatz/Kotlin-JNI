package com.dshatz.kni

import com.dshatz.kni.binding.jint
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
expect class JavaVMAttachArgs : CStructVar {

    var version: jint
    var name: CPointer<ByteVar>?

    @Deprecated("Deprecated in actual type")
    companion object : Type
}