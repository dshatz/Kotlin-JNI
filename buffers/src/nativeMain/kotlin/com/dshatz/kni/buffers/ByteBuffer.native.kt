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
    internal var finalizer: () -> Unit,
): IByteBuffer() {
    val address: CPointer<ByteVar>
        get() = _address ?: throw BufferReleasedException()

    /**
     * Release underlying native memory.
     *
     * Calling [read] or [write] after [release] will throw [BufferReleasedException].
     *
     * @see [IByteBuffer.release]
     */
    actual override fun release() {
        if (_released) return
        _released = true
        _address = null
        retainer = null

        finalizer()
    }

    private var _released: Boolean = false

    /**
     * Non-null while this buffer is borrowed, see [wrapBorrowed]. Takes ownership of the memory and returns the finalizer to use from then on.
     */
    private var retainer: (() -> () -> Unit)? = null

    /**
     * True if this buffer is only valid until [endBorrow] is called.
     */
    val isBorrowed: Boolean
        get() = retainer != null

    /**
     * Keep this buffer usable after the call it was borrowed for returns.
     *
     * Buffers passed into a `@JniCall` function are borrowed: they are invalidated when the function returns.
     * Call [retain] during the call (on the calling thread) to keep the buffer. The caller then owns it and must call [release] when done.
     *
     * Has no effect on a buffer that is not borrowed.
     *
     * @throws BufferReleasedException if the borrow had already ended.
     */
    fun retain(): ByteBuffer {
        if (_released) throw BufferReleasedException()
        retainer?.let {
            finalizer = it()
            retainer = null
        }
        return this
    }

    /**
     * Invalidate this buffer if it is still borrowed. Called by generated code when the borrowing call returns.
     */
    fun endBorrow() {
        if (retainer != null) release()
    }
    actual override val isReleased: Boolean
        get() = _released

    @OptIn(UnsafeNumber::class)
    actual override fun writeInternal(src: ByteArray, dstOffset: Int, length: Int) {
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
                finalizer = {
                    pinned.unpin()
                }
            )
        }

        /**
         * Create the [ByteBuffer] by wrapping the given [address].
         *
         * It is the caller's responsibility to release the memory in the [finalizer] and to make sure that memory is not overwritten while this object is being used.
         */
        @DelicateBufferAPI
        fun <T> wrapAddress(address: CPointer<ByteVar>, size: Long, owner: T, finalizer: (T) -> Unit): ByteBuffer {
            return ByteBuffer(
                capacity = size,
                _address = address,
                finalizer = {
                    finalizer(owner)
                }
            )
        }

        /**
         * Create a borrowed [ByteBuffer] by wrapping the given [address], valid until [endBorrow] is called.
         *
         * [retainer] is invoked by [retain]: it must take ownership of the memory and return the finalizer that gives it up.
         */
        @DelicateBufferAPI
        fun wrapBorrowed(address: CPointer<ByteVar>, size: Long, retainer: () -> () -> Unit): ByteBuffer {
            return ByteBuffer(
                capacity = size,
                _address = address,
                finalizer = {}
            ).also {
                it.retainer = retainer
            }
        }

        fun wrapAddressMemScope(memScope: MemScope, address: CPointer<ByteVar>, size: Long): ByteBuffer {
            val buffer = ByteBuffer(
                capacity = size,
                _address = address,
                finalizer = {

                }
            )
            memScope.defer {
                buffer.release()
            }
            return buffer
        }
    }
}

@RequiresOptIn("This is delicate Buffer API. Please read the kdoc.")
annotation class DelicateBufferAPI