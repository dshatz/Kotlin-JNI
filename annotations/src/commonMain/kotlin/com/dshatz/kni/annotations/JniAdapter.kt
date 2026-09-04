package com.dshatz.kni.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class JniAdapter(
    val adapter: KClass<*>
)
