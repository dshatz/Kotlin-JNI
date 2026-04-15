package com.dshatz.kni

import com.dshatz.kni.binding.jobject
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual typealias jvalue = platform.android.jvalue

@OptIn(ExperimentalForeignApi::class)
actual var jvalue.l: jobject?
    get() = this.l
    set(value) { this.l = value }