package com.dshatz.kni

import jni.JNIInvokeInterface_
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual typealias JNIInvokeInterface = JNIInvokeInterface_