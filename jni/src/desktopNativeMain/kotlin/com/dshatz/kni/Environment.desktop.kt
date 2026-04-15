package com.dshatz.kni

import com.dshatz.kni.binding.jint
import jni.JNI_ABORT
import jni.JNI_COMMIT
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual val JNI_COMMIT: jint = JNI_COMMIT

@OptIn(ExperimentalForeignApi::class)
actual val JNI_ABORT: jint = JNI_ABORT