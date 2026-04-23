# 🛠️ Setup

[![Tests](https://github.com/dshatz/Kotlin-JNI/actions/workflows/build.yml/badge.svg)](https://github.com/dshatz/Kotlin-JNI/actions/workflows/build.yml)
![Maven Central Version](https://img.shields.io/maven-central/v/com.dshatz.kni/jni)

To use Kotlin-JNI in your project, add the dependencies to your `build.gradle.kts` file.

| Module                            | Purpose                                              |
|-----------------------------------|------------------------------------------------------|
| `%package%:%jniArtifact%`         | Common extensions for working with JNI types.        |
| `%package%:%kspArtifact%`         | KSP generator for easy bi-directional communication. |
| `%package%:%annotationsArtifact%` | Annotations used by the KSP generator.               |
| `%package%:%buffersArtifact%`     | [java.nio.ByteBuffer support](Buffers.md)            |

```toml
[versions]
kni = "%latest_version%"

[plugins]
kni = { id = "%package%", version.ref = "kni" }

[libraries]
kni = { module = "%package%:%jniArtifact%", version.ref = "kni" }
kni-processor = { module = "%package%:%kspArtifact%", version.ref = "kni" }
kni-annotations = { module = "%package%:%annotationsArtifact%", version.ref = "kni" }
kni-buffers = { module = "%package%:%buffersArtifact%", version.ref = "kni" }
```

## Setup options
 - [Gradle plugin (recommended)](#autowire)
 - [Manual](#manual-setup)

## Gradle plugin

The KNI plugin significantly simplifies building KMP apps with native targets and facilitates correct KNI integration into the project.

All plugin features are optional and independent of each other. In fact, you can attempt to skip the plugin altogether and just go with KSP.

### Autowire

```kotlin
plugins {
    alias(libs.plugins.kni)
    id("com.google.devtools.ksp")
}

kni {
    autoWire {
        kspDependency = libs.kni.ksp
    }
}
```

2 source sets will be created for better code organization:
- `jniJvm[Main|Test]` containing `jvm[*]` and `android` sourcesets
- `jniNative[Main|Test]` containing `linux[*}`, `macos[*]`, `mingw[*]`, `androidNative[*]` sourcesets.

Additionally, the provided ksp dependency will be added to all relevant targets.

### Automatic Kotlin/Native bundling
Configure shared libraries built from your Kotlin/Native targets to be automatically bundled into the final JAR/AAR.

```kotlin
```
{src="plugin/bundling.kt"}

### Define optional Kotlin targets
Controlling which Kotlin targets are enabled can be vital for faster development and CI jobs.

```kotlin
```
{src="plugin/optional.kt"}

> **_NOTE:_**  All targets defined **inside** `optionalTargets` are registered unless you provide the target whitelist. 
{style="info"}

> **_NOTE:_**  All targets defined directly inside the `kotlin {}` block are always registered.
{style="info"}

> **_NOTE:_**  Dynamically disabling Android and JVM targets is not supported.
{style="warning"}

#### Pass the whitelist of targets

|                     | Example                                                              |
|---------------------|----------------------------------------------------------------------|
| gradle property     | `./gradlew allTests -PkniAllowedTargets=linuxX64,androidNativeArm64` |
| `local.properties`  | `kniAllowedTargets=linuxX64,androidNativeArm64`                      |


To configure a source set that is optional, use `gettingOptional`:
```Kotlin
sourceSets {
    val jsMain by gettingOptional {
        dependencies.implementation("...")
    }
}
```

## Manual setup
> **_NOTE:_**  This is only necessary when not using [KNI Plugin Autowire](Setup.md#autowire). 
{style="warning"}

Manual setup only requires adding the ksp processor to all targets.

```kotlin
dependencies {
    add("kspLinuxX64", libs.kni.ksp)
    add("kspAndroidNativeArm64", libs.kni.ksp)
    add("kspJvm", libs.kni.ksp)
    add("kspAndroid", libs.kni.ksp)
}
```

<seealso style="cards">
       <category ref="kni">
           <a href="Extensions.md#loading-native-libraries-jvm-android">Loading native libs from JVM</a>
           <a href="CallingNative.md">Making JNI Calls</a>
       </category>
</seealso>