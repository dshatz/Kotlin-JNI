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

- Unified JNI API: Write common JNI code for `androidNative`, `linux`, `macos`, and `mingwX64` targets.
- Type Conversion Utilities: A rich set of extension functions to effortlessly convert between JNI types and standard Kotlin types (e.g., `jstring.toKString()`, `IntArray.toJIntArray()`).
- KSP Code Generation: Automatically generate JNI-compatible C-style function stubs from your idiomatic Kotlin functions using the `@JNIConnect` annotation.
- Seamless Java Integration: Call your native Kotlin functions directly from Java with standard Kotlin types, just as you would with any other external method.
## 🛠️ Setup & Usage

Please read the [full documentation](https://dshatz.github.io/Kotlin-JNI).

## Contributions
Feel free to create Pull Requests and Issues. I will do my best to address them.