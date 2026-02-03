# 🛠️ Setup

[![Tests](https://github.com/dshatz/Kotlin-JNI/actions/workflows/build.yml/badge.svg)](https://github.com/dshatz/Kotlin-JNI/actions/workflows/build.yml)
![Maven Central Version](https://img.shields.io/maven-central/v/com.dshatz.kni/jni)

To use Kotlin-JNI in your project, add the dependencies to your `build.gradle.kts` file.

| Module                             | Purpose                                              |
|------------------------------------|------------------------------------------------------|
| `%package%:%jniArtifact%`          | Common extensions for working with JNI types.        |
| `%package%:%kspArtifact%`          | KSP generator for easy bi-directional communication. |
| `%package%:%annotationsArtifact%`  | Annotations used by the KSP generator.               |

```toml
[versions]
kni = "%latest_version%"

[libraries]
kni = { module = "%package%:%jniArtifact%", version.ref = "kni" }
kni-processor = { module = "%package%:%kspArtifact%", version.ref = "kni" }
kni-annotations = { module = "%package%:%annotationsArtifact%", version.ref = "kni" }

```
## Java Native Interface

Using the common JNI library is fairly easy, just add the dependency for your native targets.

```kotlin
val commonMain by getting {
    dependencies {
        implementation(libs.kni)
    }
}
```

## KSP

Using the KSP module to auto-generate JNI-compatible function stubs can be easily done as well.  
First ensure your project is configured for using KSP.

```kotlin
plugins {
    id("com.google.devtools.ksp")
}
```

Then add the KSP processor and annotations to your module's dependencies.
```kotlin
val commonMain by getting {
    dependencies {
        // Add the annotations dependency for KSP
        implementation(libs.kni.annotations)
    }
}

dependencies {
    // Add the processor for the native targets you need.
    add("kspLinuxX64", libs.kni.processor)
    add("kspLinuxArm64", libs.kni.processor)
    
    add("kspMingwX64", libs.kni.processor)
    
    add("kspMacosArm64", libs.kni.processor)

    add("kspAndroidNativeX64", libs.kni.processor)
    add("kspAndroidNativeArm64", libs.kni.processor)
    add("kspAndroidNativeArm32", libs.kni.processor)
}
```