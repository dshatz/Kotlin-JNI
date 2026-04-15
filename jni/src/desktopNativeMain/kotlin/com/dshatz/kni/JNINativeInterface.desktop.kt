package com.dshatz.kni

import jni.JNINativeInterface_
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual typealias JNINativeInterface = JNINativeInterface_