package dev.datlag.nkommons

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Wrapper around a direct byte buffer address.
 */
@OptIn(ExperimentalForeignApi::class)
value class ByteBuffer(val data: Pair<CPointer<ByteVar>, Long>) {
    val address get() = data.first
    val size get() = data.second

    constructor(address: CPointer<ByteVar>, size: Long): this(address to size)
}