package com.dshatz.kni

import jni.JavaVMInitArgs
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual typealias JavaVMInitArgs = JavaVMInitArgs