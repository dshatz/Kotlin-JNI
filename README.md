# Kotlin-JNI

![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-purple?logo=kotlin)
![Android Native](https://img.shields.io/badge/Android%20Native-Supported-green?logo=android)
![Linux](https://img.shields.io/badge/Linux-Supported-green?logo=linux)
![MacOS](https://img.shields.io/badge/MacOS-Supported-green?logo=apple)
![Windows](https://img.shields.io/badge/Windows-Supported-green)
![GitHub top language](https://img.shields.io/github/languages/top/dshatz/Kotlin-JNI)
![GitHub branch check runs](https://img.shields.io/github/check-runs/dshatz/Kotlin-JNI/master?nameFilter=Test%20Results)
[![licence](https://img.shields.io/badge/license-Apache%202%20-blue)](https://github.com/DatL4g/Native-Kommons/blob/master/LICENSE)
[![github stars](https://img.shields.io/github/stars/dshatz/Kotlin-JNI?style=social&label=Kotlin-KNI)](https://github.com/dshatz/Kotlin-JNI)

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

[Read more](https://dshatz.github.io/Kotlin-JNI/extensions.html#extensions)

### 3. Gradle plugin with KSP

Automatic wiring of Kotlin source sets, bundling Kotlin/Native artifacts and loading those at runtime.

[Read more](https://dshatz.github.io/Kotlin-JNI/setup.html#gradle-plugin)

### 4. Bi-directional integration
You can use standard Kotlin types for making calls between JVM and Native.
 - [Call your Kotlin/Native code from JVM using `@JniCall`](https://dshatz.github.io/Kotlin-JNI/callingnative.html)
 - [Callback from Kotlin/Native to JVM using `@JniCallback`](https://dshatz.github.io/Kotlin-JNI/callingjvm.html)

Suspend functions are supported too!

### 5. Wide type support
Pass Kotlin primitives, Strings, ByteArrays, as well as [ByteBuffers](https://dshatz.github.io/Kotlin-JNI/buffers.html) and custom types using [serializers](https://dshatz.github.io/Kotlin-JNI/customdata.html).

#### What passing data costs

Copies made when a value crosses between JVM and native, as a `@JniCall` parameter (JVM → native) or return value (native → JVM):

| Type | JVM → native | Native → JVM | Wiped |
|---|---|---|---|
| `String` | 2 copies: UTF-16 chars into a `CharArray` (`GetStringRegion`), then a Kotlin/Native `String`. | 2 copies: a temporary native UTF-16 string, then the JVM `String` (`NewString`). | No. Strings are immutable on both sides. |
| `ByteArray` | 2 copies: `GetByteArrayElements` (a copy on HotSpot; ART may return the array itself), then a Kotlin/Native `ByteArray`. | 1 copy: a new JVM array (`SetByteArrayRegion`). | No. |
| `ByteBuffer`, direct (`allocateBuffer`, `java.nio.ByteBuffer.allocateDirect`) | No copy: `address` on native points to the same memory. Borrowed: valid until the call returns, unless you call `retain()`. | No copy: the JVM gets a direct buffer over the native memory (`NewDirectByteBuffer`), which must stay allocated while the JVM uses it. | Only by you: `wipe()`, or `allocateBuffer(size, wipeOnRelease = true)` to wipe on `release()`. |
| `ByteBuffer`, heap (`java.nio.ByteBuffer.wrap`, `allocate`) | Not supported: throws `IllegalArgumentException`. Use a direct buffer. | n/a | n/a |

For sensitive data such as passwords, use a direct `ByteBuffer` and wipe it. `"secret".encodeToByteBuffer(nullTerminated = true, wipeOnRelease = true)` encodes UTF-8 straight into a new buffer, without an intermediate `ByteArray`; the source `String` itself still stays in memory until it is garbage-collected.

## 🛠️ Setup & Usage

Please read the [full documentation](https://dshatz.github.io/Kotlin-JNI).

## Contributions
Feel free to create Pull Requests and Issues. I will do my best to address them.

## This library is used by
 - [PDFMP](https://github.com/dshatz/pdfmp) - Kotlin Multiplatform PDF Library.
