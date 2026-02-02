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
C3SGXARS6
## 🛠️ Setup

To use Kotlin-JNI in your project, add the dependencies to your `build.gradle.kts` file.

### Java Native Interface

Using the common JNI library is fairly easy, just add the dependency for your native targets.

```kotlin
kotlin {
    /* --- BEGINNING OF SUPPORTED TARGETS --- */
    androidNativeX64()
    androidNativeArm64()
    androidNativeX86()
    androidNativeArm32()

    linuxX64()
    linuxArm64()
    mingwX64()
    macosX64()
    macosArm64()
    /* --- ENDING OF SUPPORTED TARGETS --- */
    
    sourceSets {
        val nativeMain by getting {
            dependencies {
                // Add the core JNI utilities
                implementation("dev.datlag.nkommons:jni:<version>")
            }
        }
    }
}
```

### KSP

Using the KSP module to auto-generate JNI-compatible function stubs can be easily done as well.  
First ensure your project is configured for using KSP.

```kotlin
plugins {
    id("com.google.devtools.ksp")
}
```

Then add the KSP processor and annotations to your module's dependencies
```kotlin
kotlin {
    sourceSets {
        val nativeMain by getting {
            dependencies {
                // Add the annotations dependency for KSP
                implementation("dev.datlag.nkommons:annotations:<version>")
            }
        }
    }
}

dependencies {
    ksp("dev.datlag.nkommons:ksp:<version>")
}
```

## Usage

Kotlin-JNI offers two main features that can be used together or independently: JNI Utilities and the `@JNIConnect` Annotation Processor.

### JNI Utilities

The library provides a set of handy extension functions to make conversions between JNI and Kotlin types trivial.

Simply import the functions you need and call them on the respective types. The JNIEnv pointer is required for operations that interact with the JVM.

#### Example: Converting `jstring` to a Kotlin `String`

```kotlin
import dev.datlag.nkommons.utils.*
import dev.datlag.nkommons.JNIEnvVar
import dev.datlag.nkommons.binding.jstring
import kotlinx.cinterop.CPointer

@CName("Java_your_package_name_ClassName_printMessage")
fun printMessage(env: CPointer<JNIEnvVar>, clazz: jobject, message: jstring) {
    val kotlinString: String? = message.toKString(env)
    
    println("Message from Java: $kotlinString")
}
```

### KSP Auto-Generating JNI Stubs with `@JNIConnect`

The `@JNIConnect` annotation is the core of the KSP module. It generates the necessary JNI boilerplate so you can write clean, idiomatic Kotlin code.

#### 1. Write Your Kotlin/Native Function

Write your function using standard Kotlin types and annotate it with `@JNIConnect`.

```kotlin
import dev.datlag.nkommons.JNIConnect

@JNIConnect
@JNIPackageName("your.package.name")
@JNIClassName("YourClass")
@JNIFunctionName("customFunction")
fun example(a: String, b: Boolean, c: CharArray, d: Double): String {
    return "$a, $b, $c, $d"
}
```

> [!WARNING]
> The provided `@JNIPackageName`, `@JNIClassName` and `@JNIFunctionName` annotations will be removed.  
> Their parameters will be merged into `@JNIConnect`.  
> Waiting for KSP fix: https://github.com/google/ksp/issues/2356

#### 2. Let KSP generate the JNI Stub

When you build the project, KSP will automatically generate a JNI-compatible function. You don't need to touch this generated file.

#### 3. Declare and Use in JVM

Now, you can declare and call the original function in your Java or JVM Kotlin code as if it were a simple external method.

```kotlin
package your.package.name

object YourClass {
    // same signature as native
    external fun customFunction(a: String, b: Boolean, c: CharArray, d: Double): String
}
```

#### Supported Types

Following types are currently supported for the auto generation:

|  Type   |  ArrayType   |
|:-------:|:------------:|
| Boolean | BooleanArray |
|  Byte   |  ByteArray   |
|  Char   |  CharArray   |
| Double  | DoubleArray  |
|  Float  |  FloatArray  |
|   Int   |   IntArray   |
|  Long   |  LongArray   |
|  Short  |  ShortArray  |
| String  |              |