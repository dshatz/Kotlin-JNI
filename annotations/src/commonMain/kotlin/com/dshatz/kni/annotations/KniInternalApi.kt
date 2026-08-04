package com.dshatz.kni.annotations

@RequiresOptIn(message = "This API is intended exclusively for KNI generated code.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
public annotation class KniInternalApi