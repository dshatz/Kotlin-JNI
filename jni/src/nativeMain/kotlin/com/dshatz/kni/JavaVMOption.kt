package com.dshatz.kni

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
expect class JavaVMOption : CStructVar {

    var extraInfo: COpaquePointer?
    var optionString: CPointer<ByteVar>?

    @Deprecated("Deprecated in actual type")
    companion object : Type
}