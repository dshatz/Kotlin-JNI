package com.dshatz.kni.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class JniSerializable(
    val with: KClass<*> = Any::class // Default value indicates that auto-generated serializer is used
)
