package com.dshatz.kni.utils

import com.dshatz.kni.binding.jobject
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Casts common jobject to jni.jobject
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalForeignApi::class)
fun jobject.forPlatform(): jni.jobject {
    return this as jni.jobject
}

/**
 * Casts jni.jobject to common jobject
 */
@OptIn(ExperimentalForeignApi::class)
fun jni.jobject.forCommon(): jobject {
    return this as jobject
}