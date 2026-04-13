package com.dshatz.kni.buffers

import kotlinx.cinterop.*
import platform.posix.memcpy

/**
 * Wrapper around a direct byte buffer address. Use [allocateBuffer], [wrapAddress] or [wrapArray] to get an instance.
 *
 * Use this in @JniConnect annotated functions to represent a java.nio.ByteBuffer.
 */
@OptIn(ExperimentalForeignApi::class)
actual class ByteBuffer internal constructor(
    actual override val capacity: Long,
    internal var _address: CPointer<ByteVar>?,
    internal var memoryOwner: Any?,
): IByteBuffer() {
    val address: CPointer<ByteVar>
        get() = _address ?: throw BufferReleasedException()

    /**
     * Release underlying native memory.
     *
     * Calling [read] or [put] after [release] will throw [BufferReleasedException].
     *
     * @see [IByteBuffer.release]
     */
    actual override fun release() {
        _released = true
        val ptr = _address
        _address = null

        if (memoryOwner == null && ptr != null) {
            // We own the memory.
            nativeHeap.free(ptr.rawValue)
        } else {
            (memoryOwner as? Pinned<*>)?.unpin()
            memoryOwner = null
        }
    }

    private var _released: Boolean = false
    actual override val isReleased: Boolean
        get() = _released

    @OptIn(UnsafeNumber::class)
    actual override fun putInternal(src: ByteArray, dstOffset: Int, length: Int) {
        src.usePinned { pinned ->
            memcpy(address + dstOffset, pinned.addressOf(0), length.convert())
        }
    }

    @OptIn(UnsafeNumber::class)
    actual override fun readInternal(dst: ByteArray, offset: Int, length: Int) {
        dst.usePinned { pinned ->
            memcpy(pinned.addressOf(0), address + offset, length.convert())
        }
    }

    companion object {
        fun wrapArray(bytes: ByteArray): ByteBuffer {
            val pinned = bytes.pin()
            return ByteBuffer(
                bytes.size.convert(),
                pinned.addressOf(0),
                pinned
            )
        }


        fun wrapAddress(address: CPointer<ByteVar>, size: Long, memoryOwner: Any? = address): ByteBuffer {
            return ByteBuffer(
                capacity = size,
                _address = address,
                memoryOwner = memoryOwner
            )
        }
    }
}