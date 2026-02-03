@CallableFromNative
interface JvmCallback: Disposable {
    fun sayHello(): String
}