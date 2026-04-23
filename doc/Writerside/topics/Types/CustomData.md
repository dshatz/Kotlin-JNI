# Custom data
To pass arbitrary objects through JNI, define a `JniSerializer` for your type.

Implement packing and unpacking into [`kotlinx.io.Buffer`](https://kotlinlang.org/api/kotlinx-io/kotlinx-io-core/kotlinx.io/-buffer/).
```kotlin
```
{src="serializer/define.kt"}