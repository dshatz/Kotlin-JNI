# ➡️ Calling Native code from JVM

The `@JniCall` annotation is the core of the KSP module. It generates the necessary JNI boilerplate so you can write clean, idiomatic Kotlin code.

### 1. Define Your Kotlin/Native Function

Write your function using standard Kotlin types and annotate it with `@JNICall`.

```kotlin
```

{src="example/native.kt"}

### 2. Let KSP generate the JNI Stub

When you build the project, KSP will automatically generate:
 - `actual` functions for JVM/Android. 
 - native function to do the heavy JNI work.

```kotlin
```
{collapsible="true" collapsed-title="Generated jvm code" src="example/generated_jvm.kt"}

```kotlin
```
{collapsible="true" collapsed-title="Generated native code" src="example/generated_native.kt"}

### 3. Use in common code

Now you can call your `mixed` function from Android, JVM, Native or `commonMain`!
 
On non-native targets the call will be routed through JNI to your Native implementation.

```kotlin
```

{src="example/external.kt"}

# Expect/actual classes
Classes with `@JniCall` annotated work seamlessly across JVM/Native. This can be useful when having a long-lived native object is beneficial.

When an object on JVM is created, a corresponding object will be created on native. All future calls on JVM side will be routed to Native.

```kotlin
// common
expect class ImageRenderer(path: String) {
    @JniCall
    fun renderImage(width: Int, height: Int): ByteBuffer
}

// native
actual class Renderer actual constructor(path: String) {
    actual fun renderImage(width: Int, height: Int): ByteBuffer {
        // your implementation
    } 
}
// jvmMain is generated
```

Calling from JVM:
```kotlin
val renderer = ImageRenderer("~/photo.jpg")
val buffer = renderer.renderImage(1024, 768)
```

## Under the hood
1. `ImageRenderer("~/photo.jpg")` makes a JNI call to Native to construct the actual class implementation with supplied parameters.
2. `Long` pointer is returned to JVM and stored in the JVM object.
3. `renderer.renderImage` passes the pointer, along with supplied parameters to native, where the native object is requested from the pointer.

> **Note regarding state:** State (i.e. kotlin properties) is not propagated between JVM and Native in such objects.   
>
> **Recommended**: keep the state on the native side and access it using `@JniCall` annotated functions. Or even better, keep the state separately.
{style="note"}
