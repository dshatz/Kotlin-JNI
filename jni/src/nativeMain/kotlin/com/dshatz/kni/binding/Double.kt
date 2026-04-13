package com.dshatz.kni.binding

import kotlinx.cinterop.DoubleVarOf
import kotlinx.cinterop.ExperimentalForeignApi

typealias jdouble = Double

@OptIn(ExperimentalForeignApi::class)
typealias jdoubleVar = DoubleVarOf<jdouble>

@OptIn(ExperimentalForeignApi::class)
typealias jdoubleArray = jarray