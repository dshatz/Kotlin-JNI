package dev.datlag.nkommons

/**
 * Auto generate JNI compatible function from native code.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class JNIConnect(
    val packageName: String = "",
    val className: String = "",
    val functionName: String = ""
)