package com.dshatz.kni

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import kotlin.reflect.KProperty

class OptionalSourceSetDelegate(
    private val container: NamedDomainObjectContainer<KotlinSourceSet>,
    private val configure: (KotlinSourceSet.() -> Unit)? = null
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): KotlinSourceSet? {
        val sourceSet = container.findByName(property.name)
        return sourceSet?.apply {
            configure?.invoke(this)
        }
    }
}

fun NamedDomainObjectContainer<KotlinSourceSet>.gettingOptional(
    configure: (KotlinSourceSet.() -> Unit)? = null
) = OptionalSourceSetDelegate(this, configure)