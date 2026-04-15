package com.dshatz.kni.buffers

import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.js.Promise


private fun Blob.arrayBuffer(): Promise<ArrayBuffer> =
    asDynamic().arrayBuffer() as Promise<ArrayBuffer>

fun ByteBuffer.toBlob(contentType: String = "application/octet-stream"): Blob {
    val raw = this.byteArray.asDynamic()
    val uint8View = Uint8Array(raw.buffer, raw.byteOffset, raw.length)

    return Blob(arrayOf(uint8View), BlobPropertyBag(type = contentType))
}

fun ByteBuffer.asUint8Array(): Uint8Array {
    val raw = this.byteArray
    val jsArray = raw.asDynamic()

    return Uint8Array(
        jsArray.buffer,
        jsArray.byteOffset,
        jsArray.length
    )
}

fun ByteBuffer.asInt8Array(): Int8Array {
    return this.byteArray.asDynamic() as Int8Array
}

suspend fun Blob.toByteArray(): ByteArray {
    val arrayBuffer = this.arrayBuffer().await()
    val int8Array = Int8Array(arrayBuffer)
    return int8Array.asDynamic() as ByteArray
}