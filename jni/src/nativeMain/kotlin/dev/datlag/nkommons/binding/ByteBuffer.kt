package dev.datlag.nkommons.binding

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Wrapper around a direct byte buffer address.
 *
 * Use this in @JniConnect annotated functions to represent a java.nio.ByteBuffer.
 */
@OptIn(ExperimentalForeignApi::class)
actual class ByteBuffer(val address: CPointer<ByteVar>, val size: Long)