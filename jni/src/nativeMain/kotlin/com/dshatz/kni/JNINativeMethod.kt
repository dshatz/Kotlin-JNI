package com.dshatz.kni

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
expect class JNINativeMethod : CStructVar {
    var fnPtr: COpaquePointer?
    var signature: CPointer<ByteVar>?
    var name: CPointer<ByteVar>?

    @Deprecated("Deprecated in actual type")
    companion object : Type
}