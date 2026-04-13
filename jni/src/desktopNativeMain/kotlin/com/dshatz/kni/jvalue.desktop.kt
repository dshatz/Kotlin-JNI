package com.dshatz.kni

import com.dshatz.kni.binding.jobject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual typealias jvalue = jni.jvalue

@OptIn(ExperimentalForeignApi::class)
actual var jvalue.l: jobject?
    get() = this.l
    set(value) { this.l = value?.reinterpret() }