# ➡️ Calling Native code from JVM

The `@JNIConnect` annotation is the core of the KSP module. It generates the necessary JNI boilerplate so you can write clean, idiomatic Kotlin code.

### 1. Write Your Kotlin/Native Function

Write your function using standard Kotlin types and annotate it with `@JNIConnect`.

```kotlin
```

{src="example/native.kt"}

### 2. Let KSP generate the JNI Stub

When you build the project, KSP will automatically generate a JNI-compatible function. You don't need to touch this generated file.

The following will be generated:
```kotlin
```

{collapsible="true" collapsed-title="Generated _exampleJNI.kt" src="example/generated.kt"}

### 3. Declare and Use in JVM

Now, you can declare and call the original function in your Java or JVM Kotlin code as if it were a simple external method.

```kotlin
```

{src="example/external.kt"}

### Supported Types

The following types are currently supported for direct mapping:

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

The following types will be mapped to a different Native-friendly type.

| JVM                    | Native                           |
|------------------------|----------------------------------|
| `java.nio.ByteBuffer`  | `dev.datlag.nkommons.ByteBuffer` |


### Using java.nio.ByteBuffer

You can pass direct `ByteBuffer` from JVM to Native easily:

```kotlin
```
{src="fillBuffer/external.kt"}

On native side it will be mapped to `dev.datlag.nkommons.ByteBuffer`. You can access the address of the buffer, as well as its size. 
```kotlin
```
{src="fillBuffer/native.kt"}