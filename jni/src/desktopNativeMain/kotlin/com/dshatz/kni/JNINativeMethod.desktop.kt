package com.dshatz.kni

import jni.JNINativeMethod
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual typealias JNINativeMethod = JNINativeMethod