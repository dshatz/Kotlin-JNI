# Custom data
You can pass custom objects through JNI boundary by marking them with `JniSerializable` annotation.
The KNI processor will generate serializers to convert your data from and to a `ByteArray`.

```kotlin
```
{src="serializer/define.kt"}

You can then use this type in your JNI calls:
```kotlin
// commonMain
@JniCall
expect fun passObject(obj: ColorfulObject): Amount


// nativeMain
@JniCall 
actual fun passObject(obj: ColorfulObject): Amount {
    return if (obj.color == "orange") Amount(150)
    else if (obj.color == "green") Amount(100)
    else Amount(80)
}
```

## Supported class types:
 - `data class`
 - `value class`
 - `sealed class` - polymorphic serializer will be generated

> **_BEWARE:_**  Classes should not have mutable state. Only properties defined within the constructor will be serialized.
{style="warning"}


## Customizing serialization for properties
Apply a `@JniSerializable(with = ...)` annotation to a property of data class to supply your own implementation of a `JniSerializer`.