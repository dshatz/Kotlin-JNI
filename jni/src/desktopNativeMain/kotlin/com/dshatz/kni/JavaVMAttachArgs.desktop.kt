package com.dshatz.kni

import jni.JavaVMAttachArgs
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual typealias JavaVMAttachArgs = JavaVMAttachArgs