package com.dshatz.kni

import jni.JavaVMOption
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual typealias JavaVMOption = JavaVMOption