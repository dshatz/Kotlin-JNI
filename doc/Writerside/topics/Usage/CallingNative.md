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