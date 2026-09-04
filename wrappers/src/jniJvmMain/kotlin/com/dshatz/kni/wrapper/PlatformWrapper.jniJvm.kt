package com.dshatz.kni.wrapper


interface JvmJniAdapter<T, JNI>: JniAdapter<T> {
    fun getJniValue(value: T): JNI
    fun fromJniValue(value: JNI): T
}