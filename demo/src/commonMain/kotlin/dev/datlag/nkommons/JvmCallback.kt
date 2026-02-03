package dev.datlag.nkommons

@CallableFromNative
interface JvmCallback: Disposable {
    fun sayHello(): String
}