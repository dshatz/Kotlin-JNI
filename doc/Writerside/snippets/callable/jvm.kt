// JVM / Android
class JvmCallbackImpl: JvmCallback {
    override fun sayHello(): String = "Hello"
    override fun close() {
        // This will be automatically called when the
        // native side calls close().

        // Clean up resources on the JVM side.
    }
}