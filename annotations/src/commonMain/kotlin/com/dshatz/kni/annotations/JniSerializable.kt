package com.dshatz.kni.annotations

import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

@Target(PROPERTY, CLASS, TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class JniSerializable(
    val with: KClass<*> = Any::class // Default value indicates that auto-generated serializer is used
)
