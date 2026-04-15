package com.dshatz.kni

import org.gradle.api.Project
import java.util.Properties

internal fun Project.localProperties(): Properties {
    val localProps = Properties()
    rootProject.layout.settingsDirectory.file("local.properties").asFile.inputStream().use {
        localProps.load(it)
    }
    return localProps
}

internal object Config {
    const val ARG_ALLOWED_TARGETS = "kniAllowedTargets"
    const val ARG_NATIVE_BUILD_TYPE = "kniNativeBuildType"
}