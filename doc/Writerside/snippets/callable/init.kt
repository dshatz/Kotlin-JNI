lateinit var jvmCallback: JvmCallback
// commonMain
@JniCall expect fun init(callback: JvmCallback)
@JniCall expect fun dispose()

// Native
actual fun init(callback: JvmCallback) {
    // Save the object for later.
    jvmCallback = callback
}

actual fun dispose() {
    jvmCallback.dispose()
}