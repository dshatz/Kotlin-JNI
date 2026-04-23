package com.dshatz.kni.annotations

/**
 * Annotate an interface in commonMain with this to be able to call the methods from native.
 *
 * You can then pass an object implementing this interface from JVM to Native via [JniCall]
 * and native will be able to call the methods of this object.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class JniCallback