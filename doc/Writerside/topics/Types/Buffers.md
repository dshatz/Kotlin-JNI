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
