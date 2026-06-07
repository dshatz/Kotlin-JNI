package com.dshatz.kni.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class JniSerializerFor(
    val target: KClass<*>
)