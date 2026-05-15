package com.dshatz.kni.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class JniSerializerFor(
    val target: KClass<*>
)