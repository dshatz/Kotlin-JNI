package com.dshatz.kni.binding

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
typealias jobject = CPointer<out CPointed>

@OptIn(ExperimentalForeignApi::class)
typealias jobjectVar = CPointerVarOf<jobject>

@OptIn(ExperimentalForeignApi::class)
typealias jarray = jobject

@OptIn(ExperimentalForeignApi::class)
typealias jobjectArray = jarray

@OptIn(ExperimentalForeignApi::class)
typealias jobjectArrayVar = CPointerVarOf<jobjectArray>

@OptIn(ExperimentalForeignApi::class)
typealias jthrowable = jobject

@OptIn(ExperimentalForeignApi::class)
typealias jthrowableVar = CPointerVarOf<jthrowable>

@OptIn(ExperimentalForeignApi::class)
typealias jstring = jobject

@OptIn(ExperimentalForeignApi::class)
typealias jstringVar = CPointerVarOf<jstring>

@OptIn(ExperimentalForeignApi::class)
typealias jweak = jobject

@OptIn(ExperimentalForeignApi::class)
typealias jweakVar = CPointerVarOf<jweak>

@OptIn(ExperimentalForeignApi::class)
typealias jclass = jobject

@OptIn(ExperimentalForeignApi::class)
typealias jclassVar = CPointerVarOf<jclass>

@OptIn(ExperimentalForeignApi::class)
typealias jfieldID = CPointer<out CPointed>

@OptIn(ExperimentalForeignApi::class)
typealias jfieldIDVar = CPointerVarOf<jfieldID>

@OptIn(ExperimentalForeignApi::class)
typealias jmethodID = CPointer<out CPointed>

@OptIn(ExperimentalForeignApi::class)
typealias jmethodIDVar = CPointerVarOf<jmethodID>

typealias jobjectRefType = UInt