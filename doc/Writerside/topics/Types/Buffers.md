# java.nio.ByteBuffer

While passing `java.nio.ByteBuffer` is not possible because such type does not exist on Native, the `buffers` module provides an efficient wrapper.

Use `com.dshatz.kni.buffers.ByteBuffer` in function signatures:

## Setup
Add the dependency:
```kotlin
implementation("com.dshatz.kni:buffers:%latest_version%")
```

Buffers module provides a convenient and efficient wrapper around byte buffers for all platforms.

## Allocating / releasing
```kotlin
val buffer: ByteBuffer = allocateBuffer(1024)
buffer.release()
```

## Reading / writing

```kotlin
val bytes: ByteArray = Random.nextBytes(buffer.capacity.toInt())
buffer.write(bytes)
```

```kotlin
val bytes: ByteArray = buffer.readToByteArray(0, 1024)
```

## Properties
The following properties are available on the `ByteBuffer`:

| Property                       | Platforms   |
|--------------------------------|-------------|
| `capacity: Long`                 | All         |
| `jvmBuffer: java.nio.ByteBuffer` | JVM/Android |
| `val address: CPointer<ByteVar>` | Native      |


## Passing to JNI
See the general guide:
<a href="CallingNative.md">
</a>


```kotlin
```
{src="fillBuffer/define.kt"}

Call from `commonMain`:
```kotlin
```
{src="fillBuffer/call.kt"}

## Buffer lifetime on native
A `ByteBuffer` received as a parameter of a `@JniCall` function, or of the constructor of a class with `@JniCall` functions, is only borrowed:
it is invalidated when the function or constructor returns, and using it afterwards throws `BufferReleasedException`.

This applies to `suspend` functions too: the buffer stays valid until the suspend function completes, even though that is after the underlying JNI call has returned.
The JVM buffer is kept alive for that whole time, even if the caller no longer references it.

To keep the buffer, call `retain()` during the call. You then own it and must call `release()` when done:
```kotlin
@JniCall
actual fun keep(buffer: ByteBuffer) {
    stored = buffer.retain()
}
```

> In a blocking function or constructor, call `retain()` on the thread it was called on. In a `suspend` function, it can be called from any thread while the function is running.

Storing a buffer received in a constructor works the same way:
```kotlin
actual class Holder actual constructor(buffer: ByteBuffer) : AutoCloseable {
    private val buffer = buffer.retain()

    actual override fun close() {
        buffer.release()
    }
}
```

## Direct buffers only
Native code can only access direct buffers. Passing a buffer that wraps a heap `java.nio.ByteBuffer` (e.g. `java.nio.ByteBuffer.allocate(n).toCommonByteBuffer()`) to native code throws `IllegalArgumentException`.
Use `allocateBuffer` or `java.nio.ByteBuffer.allocateDirect` instead.

## Converting
### JVM
```kotlin 
val jvmBuffer = java.nio.ByteBuffer.allocateDirect(1024)
val buffer = jvmBuffer.toCommonByteBuffer()
buffer.jvmBuffer == jvmBuffer
```

### Native
On native, this is a wrapper around a pointer.
#### Wrap ByteArray
> **_NOTE:_**  Be careful with memory ownership - `ByteBuffer.wrapArray` will `pin()` the ByteArray.
> It is important to call `ByteBuffer.release()` after use.
{style="warning"}
>
```kotlin
val bytes = Random.nextBytes(100)
val buffer = ByteBuffer.wrapArray(bytes)
```

#### Wrap address in `memScoped`
> **_NOTE:_**  `ByteBuffer` created this way will be freed when the `memScoped` block is completed.
{style="warning"}
>
```kotlin
// wrap a memory address
memScoped {
    val arr = allocArray<ByteVar>(1024)
    val buffer = ByteBuffer.wrapAddressMemScope(this, arr, 1024)
}
```

### JS/Wasm

```kotlin
val buffer = allocateBuffer(1024)
buffer.asInt8Array()
buffer.toBlob()
```
