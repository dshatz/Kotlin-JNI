package com.dshatz.kni

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class KniExtension @Inject constructor(private val objects: ObjectFactory, private val project: Project) {
    private val createSourceSets = objects.property(Boolean::class.java).convention(false)

    fun autoWire(action: Action<AutoWireExtension>) {
        createSourceSets.set(true)
        val autoWire = objects.newInstance(AutoWireExtension::class.java)
        action.execute(autoWire)
        autoWire(project, autoWire)
    }
}

open class AutoWireExtension @Inject constructor(objects: ObjectFactory) {
    val kspDependency = objects.property(Any::class.java)
}
