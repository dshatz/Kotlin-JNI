package dev.datlag.nkommons

import dev.datlag.nkommons.binding.jboolean
import dev.datlag.nkommons.binding.jbyte
import dev.datlag.nkommons.binding.jchar
import dev.datlag.nkommons.binding.jdouble
import dev.datlag.nkommons.binding.jfloat
import dev.datlag.nkommons.binding.jint
import dev.datlag.nkommons.binding.jlong
import dev.datlag.nkommons.binding.jobject
import dev.datlag.nkommons.binding.jshort
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual typealias jvalue = jni.jvalue

@OptIn(ExperimentalForeignApi::class)
actual var jvalue.l: jobject?
    get() = this.l
    set(value) { this.l = value?.reinterpret() }