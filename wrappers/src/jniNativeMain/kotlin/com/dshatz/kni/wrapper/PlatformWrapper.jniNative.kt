package com.dshatz.kni.wrapper

import com.dshatz.kni.JNIEnvVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi


@OptIn(ExperimentalForeignApi::class)
interface NativeJniAdapter<T, Native>: JniAdapter<T> {
    fun fromJni(env: CPointer<JNIEnvVar>, value: Native): T
    fun toJni(env: CPointer<JNIEnvVar>, value: T): Native
}