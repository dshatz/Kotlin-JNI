package dev.datlag.nkommons.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import platform.posix.memcpy
import platform.posix.size_t

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
fun memcpy(
    dst: kotlinx.cinterop.CValuesRef<*>,
    src: kotlinx.cinterop.CValuesRef<*>,
    size: ULong
): kotlinx.cinterop.CPointer<out kotlinx.cinterop.CPointed>? {
    return memcpy(dst, src, size.convert<size_t>())
}