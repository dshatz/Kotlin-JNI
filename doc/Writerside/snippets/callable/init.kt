lateinit var jvmCallback: JvmCallback
// Native
@JNIConnect(
    packageName = "com.example",
    className = "MainKt"
)
fun init(callback: JvmCallback) {
    // Save the object for later.
    jvmCallback = callback
}

@JNIConnect(
    packageName = "com.example",
    className = "MainKt"
)
fun dispose() {
    jvmCallback.dispose()
}