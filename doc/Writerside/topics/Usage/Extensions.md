# ⚙️ Kotlin JNI Library

## Loading native libraries *(JVM/Android)*

The `jni` module provides a helper for seamless integration with the [Gradle Plugin](Setup.md#gradle-plugin).

This will call `System.loadLibrary()`, with some pre-processing and name calculation depending on the platform.

```kotlin
BundledLibLoader.loadBundledLibrary("demo")
```

> **_NOTE:_**  Library name (`demo`) is typically the same as your module name. Refer to [kotlin docs](https://kotlinlang.org/docs/multiplatform/multiplatform-build-native-binaries.html#declare-binaries) for customization.
{style="info"}
 


## Extensions
Some of these extensions can be useful when not using the [KSP generator](CallingNative.md) and writing your own `@CName`-annotated functions.

## Converting from JNI types to Kotlin types

Converting between JNI, Kotlin and back is easy:
```kotlin
```
{src="extensions/converting.kt"}

For a detailed list of functions, see the [API Reference](https://dshatz.github.io/Kotlin-JNI/api/).

