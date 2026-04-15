package com.dshatz.kni

import org.gradle.api.Project
import java.util.Properties

internal fun Project.localProperties(): Properties {
    val localProps = Properties()
    runCatching {
        rootProject.layout.settingsDirectory.file("local.properties").asFile.inputStream().use {
            localProps.load(it)
        }
    }
    return localProps
}

fun Project.getKniProperty(name: String): String? {
    val gradleProp = if (project.rootProject.hasProperty(name)) project.rootProject.property(name) as String else null
    return gradleProp ?: run {
        localProperties().getProperty(name, null)
    }
}

internal object Config {
    const val ARG_ALLOWED_TARGETS = "kniAllowedTargets"
    const val ARG_DISABLED_TARGETS = "kniAllowedTargets"
    const val ARG_NATIVE_BUILD_TYPE = "kniNativeBuildType"
}