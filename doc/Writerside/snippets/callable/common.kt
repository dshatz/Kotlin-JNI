@CallableFromNative
interface JvmCallback: AutoCloseable {
    fun sayHello(): String
}