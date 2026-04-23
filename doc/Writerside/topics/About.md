# Kotlin-JNI

Kotlin-JNI is a powerful Kotlin Multiplatform library designed to simplify and unify JNI (Java Native Interface) development across Android, Desktop (Linux, macOS, Windows), and Kotlin/Native.

It provides a common JNI interface for all supported native targets and includes a KSP module to automatically generate JNI-compatible function stubs, eliminating boilerplate and bridging the gap between your native Kotlin code and the Java world.

## Acknowledgement

I want to express great gratitude to the original creator @DatL4g for the idea and execution. This project indeed makes JNI on Kotlin painless and effortless.

However, since the original repository no longer has a permissive license, this repository had to be created, based on the older, Apache-licensed version.

## ✨ Features

### 1. Unified JNI API

Use expect/actual definitions to seamlessly integrate JVM and Native code.

Works across `androidNative`, `linux`, `macos`, and `mingwX64` targets.

### 2. Type conversion utilities

A rich set of extension functions to effortlessly convert between JNI types and standard Kotlin types (e.g., `jstring.toKString()`, `IntArray.toJIntArray()`).

### 3. Gradle plugin with KSP

Automatic wiring of Kotlin source sets, bundling Kotlin/Native artifacts and loading those at runtime.

Automatically generate JNI-compatible C-style function stubs from your idiomatic Kotlin functions using the [`@JniCall` annotation](CallingNative.md).

### 4. Bi-directional integration
You can use standard Kotlin types for making calls between JVM and Native.
 - [Call your Kotlin/Native code from JVM using `@JniCall`](CallingNative.md)
 - [Callback from Kotlin/Native to JVM using `@JniCallback`](CallingJvm.md)

### 5. Wide type support
Pass Kotlin primitives, Strings, ByteArrays, as well as [ByteBuffers](Buffers.md) and custom types using [serializers](CustomData.md).

<seealso style="cards">
    <category ref="kni">
           <a href="Setup.md"/>
       </category>
</seealso>