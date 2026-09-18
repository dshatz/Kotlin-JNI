# Kotlin-JNI

![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-purple?logo=kotlin)
![Android Native](https://img.shields.io/badge/Android%20Native-Supported-green?logo=android)
![Linux](https://img.shields.io/badge/Linux-Supported-green?logo=linux)
![MacOS](https://img.shields.io/badge/MacOS-Supported-green?logo=apple)
![Windows](https://img.shields.io/badge/Windows-Supported-green)
![Language](https://img.shields.io/github/languages/top/DatL4g/Native-Kommons)
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

### 5. Wide type support
Pass Kotlin primitives, Strings, ByteArrays, as well as [ByteBuffers](https://dshatz.github.io/Kotlin-JNI/buffers.html) and custom types using [serializers](https://dshatz.github.io/Kotlin-JNI/customdata.html).

## 🛠️ Setup & Usage

Please read the [full documentation](https://dshatz.github.io/Kotlin-JNI).

## Contributions
Feel free to create Pull Requests and Issues. I will do my best to address them.
