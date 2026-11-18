package kni.test

import com.dshatz.kni.annotations.JniCallback

/**
 * Example callback that native can call to request some data.
 *
 * [requestInt] can run asynchronously on JVM, calling [NativeCallback] when it is done.
 */
@JniCallback
interface RequestIntCallback: AutoCloseable {
    fun requestInt(nativeCallback: NativeCallback)
}
