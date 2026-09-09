package com.dshatz.kni.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class TypeMarker(vararg val packages: String)