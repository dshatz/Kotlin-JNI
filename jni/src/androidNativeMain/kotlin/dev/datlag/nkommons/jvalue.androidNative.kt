package dev.datlag.nkommons

import dev.datlag.nkommons.binding.jobject
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual typealias jvalue = platform.android.jvalue

@OptIn(ExperimentalForeignApi::class)
actual var jvalue.l: jobject?
    get() = this.l
    set(value) { this.l = value }