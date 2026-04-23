@JniCallback
interface JvmCallback: AutoCloseable {
    fun sayHello(): String
}