package com.dshatz.kni

import com.dshatz.kni.binding.jboolean
import com.dshatz.kni.binding.jbyte
import com.dshatz.kni.binding.jchar
import com.dshatz.kni.binding.jdouble
import com.dshatz.kni.binding.jfloat
import com.dshatz.kni.binding.jint
import com.dshatz.kni.binding.jlong
import com.dshatz.kni.binding.jobject
import com.dshatz.kni.binding.jshort
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
expect class jvalue : CStructVar {

    var f: jfloat
    var c: jchar
    var d: jdouble
    var b: jbyte
    var j: jlong
    var s: jshort
    var z: jboolean
    var i: jint

    @Deprecated("Deprecated in actual type")
    companion object : CStructVar.Type
}

@OptIn(ExperimentalForeignApi::class)
expect var jvalue.l: jobject?