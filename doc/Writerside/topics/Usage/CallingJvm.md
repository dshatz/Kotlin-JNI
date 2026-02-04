# ⬅️ Calling JVM code from Native

Calling back is easy too!

### 1. Create a contract interface in `commonMain`
```kotlin
```

{ src="callable/common.kt" }

The `@CallableFromNative` annotation will tell KSP to generate relevant bindings on the native side.

```kotlin
```

{collapsible="true" collapsed-title="Generated _JvmCallbackNativeImpl.kt" src="callable/generated.kt"}

> **_NOTE:_**  If you want to be passing ByteBuffers, use `dev.datlag.nkommons.ByteBuffer`. 
{style="note"}
> 
### 2. Create the JVM implementation
```kotlin
```

{src="callable/jvm.kt"}

### 3. Pass your JVM object to Native

> **_NOTE:_**  The Native side needs the callback object to be able to call its functions.
> You can pass a `JvmCallbackImpl` object to native via the [@JNIConnect mechanism](CallingNative.md). Example below.
{style="note"}

```kotlin
```
{ src="callable/initExternal.kt" }

```kotlin
```
{ src="callable/init.kt" }

```kotlin
```
{collapsible="true" collapsed-title="Generated _initJNI.kt" src="callable/initGenerated.kt"}

```kotlin
```
{collapsible="true" collapsed-title="Generated _disposeJNI.kt" src="callable/disposeGenerated.kt"}

> **Critical:** Do not forget to call `callback.dispose()` when you no longer need it.
> 
> As soon as the object arrives to Native, a JNI [`GlobalRef` is created](https://dshatz.github.io/Kotlin-JNI/api/jni/dev.datlag.nkommons.utils/-new-global-ref.html). 
> Therefore, it's essential to free it later by using `dispose()` to avoid memory leaks and other nasty problems. 
{style="warning"}

> **Note:** Calling `dispose()` in Native will also call `dispose()` on the JVM object. No need to call it manually.
> 
> If you attempt to use the native object after calling `dispose()`, you will get an error.
{style="note"}

### 4. Call `sayHello()` from native!
```kotlin
// Native

fun getGreetings() {
    val message = jvmCallback.sayHello()
    memScoped {
        fprintf(stderr, "Message from JVM: %s\n", message.cstr.ptr)
    }
    // Greetings received, dispose of the object
    jvmCallback.dispose()
}

