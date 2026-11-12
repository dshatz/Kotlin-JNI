package kni.test

import com.dshatz.kni.annotations.JniCall

abstract class AbstractBase {
    abstract fun describe(): String
    fun greeting(): String = "Hello from ${describe()}"
}

expect class ChildOfAbstract(value: Int) : AbstractBase, AutoCloseable {
    @JniCall
    fun getValue(): Int

    @JniCall
    override fun describe(): String

    override fun close()
}

expect object ObjectOfAbstract : AbstractBase {
    @JniCall
    override fun describe(): String
}
