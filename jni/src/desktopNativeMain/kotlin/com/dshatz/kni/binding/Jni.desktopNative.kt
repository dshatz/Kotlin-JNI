package com.dshatz.kni.binding

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual val JNI_VERSION_1_6: jint = jni.JNI_VERSION_1_6
@OptIn(ExperimentalForeignApi::class)
actual val JNI_EDETACHED: jint = jni.JNI_EDETACHED
@OptIn(ExperimentalForeignApi::class)
actual val JNI_OK: jint = jni.JNI_OK