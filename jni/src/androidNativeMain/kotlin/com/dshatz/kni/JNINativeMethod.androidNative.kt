package com.dshatz.kni

import kotlinx.cinterop.ExperimentalForeignApi
import platform.android.JNINativeMethod

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual typealias JNINativeMethod = JNINativeMethod