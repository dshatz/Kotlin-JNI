package com.dshatz.kni

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import kotlin.reflect.KProperty

class OptionalSourceSetDelegate(
    private val container: NamedDomainObjectContainer<KotlinSourceSet>,
    private val configure: (KotlinSourceSet.() -> Unit)?
) {
    private var memoized: KotlinSourceSet? = null

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): OptionalSourceSetDelegate {
        memoized = container.findByName(property.name)?.apply {
            configure?.invoke(this)
        }
        return this
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): KotlinSourceSet? = memoized
}

fun NamedDomainObjectContainer<KotlinSourceSet>.gettingOptional(
    configure: (KotlinSourceSet.() -> Unit)? = null
) = OptionalSourceSetDelegate(this, configure)