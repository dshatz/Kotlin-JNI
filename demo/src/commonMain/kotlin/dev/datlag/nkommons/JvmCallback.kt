package dev.datlag.nkommons

@CallableFromNative
interface JvmCallback: AutoCloseable {
    fun sayHello(): String
}