package dev.datlag.nkommons

/**
 * Can be disposed via [dispose] in KSP-generated Native code.
 */
interface Disposable: AutoCloseable {
    fun dispose() = close()
    override fun close() {
        // noop, only implemented on native.
    }
}