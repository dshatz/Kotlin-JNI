package dev.datlag.nkommons

/**
 * Annotate an interface in commonMain with this to be able to call the methods from native.
 *
 * You can then pass an object implementing this interface from JVM to Native via [JNIConnect]
 * and native will be able to call the methods of this object.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class CallableFromNative