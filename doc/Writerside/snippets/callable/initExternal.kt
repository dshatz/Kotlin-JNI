// JVM / Android
fun main() = init(JvmCallbackImpl())

external fun init(callback: JvmCallback)
external fun dispose()