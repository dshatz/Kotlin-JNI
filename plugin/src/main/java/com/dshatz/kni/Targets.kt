package com.dshatz.kni

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmWasiTargetDsl


open class OptionalTargetsExtension(
    private val kotlin: KotlinMultiplatformExtension
) {
    private val allowed = kotlin.project.rootProject.allowedTargets
    // Linux
    fun linuxX64(name: String = "linuxX64", configure: Action<KotlinNativeTargetWithHostTests> = {}): KotlinNativeTargetWithHostTests? {
        return if (name in allowed) kotlin.linuxX64(configure) else null
    }

    fun linuxArm64(name: String = "linuxArm64", configure: Action<KotlinNativeTarget> = {}): KotlinNativeTarget? {
        return if (name in allowed) kotlin.linuxArm64(configure) else null
    }

    // macOS
    fun macosX64(name: String = "macosX64", configure: Action<KotlinNativeTargetWithHostTests> = {}): KotlinNativeTargetWithHostTests? {
        return if (name in allowed) kotlin.macosX64(configure) else null
    }

    fun macosArm64(name: String = "macosArm64", configure: Action<KotlinNativeTargetWithHostTests> = {}): KotlinNativeTargetWithHostTests? {
        return if (name in allowed) kotlin.macosArm64(configure) else null
    }

    // windows
    fun mingwX64(name: String = "mingwX64", configure: Action<KotlinNativeTargetWithHostTests> = {}): KotlinNativeTargetWithHostTests? {
        return if (name in allowed) kotlin.mingwX64(configure) else null
    }

// Android native

    fun androidNativeArm32(name: String = "androidNativeArm32", configure: Action<KotlinNativeTarget> = {}): KotlinNativeTarget? {
        return if (name in allowed) kotlin.androidNativeArm32(name, configure) else null
    }

    fun androidNativeArm64(name: String = "androidNativeArm64", configure: Action<KotlinNativeTarget> = {}): KotlinNativeTarget? {
        return if (name in allowed) kotlin.androidNativeArm64(name, configure) else null
    }

    fun androidNativeX86(name: String = "androidNativeX86", configure: Action<KotlinNativeTarget> = {}): KotlinNativeTarget? {
        return if (name in allowed) kotlin.androidNativeX86(name, configure) else null
    }

    fun androidNativeX64(name: String = "androidNativeX64", configure: Action<KotlinNativeTarget> = {}): KotlinNativeTarget? {
        return if (name in allowed) kotlin.androidNativeX64(name, configure) else null
    }


    // iOS
    fun iosArm64(name: String = "iosArm64", configure: Action<KotlinNativeTarget> = {}): KotlinNativeTarget? {
        return if (name in allowed) kotlin.iosArm64(name, configure) else null
    }

    fun iosSimulatorArm64(name: String = "iosSimulatorArm64", configure: Action<KotlinNativeTargetWithSimulatorTests> = {}): KotlinNativeTargetWithSimulatorTests? {
        return if (name in allowed) kotlin.iosSimulatorArm64(name, configure) else null
    }

    fun iosX64(name: String = "iosX64", configure: Action<KotlinNativeTargetWithSimulatorTests> = {}): KotlinNativeTargetWithSimulatorTests? {
        return if (name in allowed) kotlin.iosX64(name, configure) else null
    }


    // watchos
    fun watchosArm32(name: String = "watchosArm32", configure: Action<KotlinNativeTarget> = {}): KotlinNativeTarget? {
        return if (name in allowed) kotlin.watchosArm32(name, configure) else null
    }

    fun watchosArm64(name: String = "watchosArm64", configure: Action<KotlinNativeTarget> = {}): KotlinNativeTarget? {
        return if (name in allowed) kotlin.watchosArm64(name, configure) else null
    }

    fun watchosSimulatorArm64(name: String = "watchosSimulatorArm64", configure: Action<KotlinNativeTargetWithSimulatorTests> = {}): KotlinNativeTargetWithSimulatorTests? {
        return if (name in allowed) kotlin.watchosSimulatorArm64(name, configure) else null
    }

    fun watchosX64(name: String = "watchosX64", configure: Action<KotlinNativeTargetWithSimulatorTests> = {}): KotlinNativeTargetWithSimulatorTests? {
        return if (name in allowed) kotlin.watchosX64(name, configure) else null
    }

    fun watchosDeviceArm64(name: String = "watchosDeviceArm64", configure: Action<KotlinNativeTarget> = {}): KotlinNativeTarget? {
        return if (name in allowed) kotlin.watchosDeviceArm64(name, configure) else null
    }


// tvos

    fun tvosArm64(name: String = "tvosArm64", configure: Action<KotlinNativeTarget> = {}): KotlinNativeTarget? {
        return if (name in allowed) kotlin.tvosArm64(name, configure) else null
    }

    fun tvosSimulatorArm64(name: String = "tvosSimulatorArm64", configure: Action<KotlinNativeTargetWithSimulatorTests> = {}): KotlinNativeTargetWithSimulatorTests? {
        return if (name in allowed) kotlin.tvosSimulatorArm64(name, configure) else null
    }

    fun tvosX64(name: String = "tvosX64", configure: Action<KotlinNativeTargetWithSimulatorTests> = {}): KotlinNativeTargetWithSimulatorTests? {
        return if (name in allowed) kotlin.tvosX64(name, configure) else null
    }

    // web
    @ExperimentalWasmDsl
    fun wasmJs(name: String = "wasmJs", configure: Action<KotlinWasmJsTargetDsl> = {}): KotlinWasmJsTargetDsl? {
        return if (name in allowed) kotlin.wasmJs(name, configure) else null
    }

    fun js(name: String = "js", configure: Action<KotlinJsTargetDsl> = {}): KotlinJsTargetDsl? {
        return if (name in allowed) kotlin.js(name, configure) else null
    }

    @ExperimentalWasmDsl
    fun wasmWasi(name: String = "wasmWasi", configure: Action<KotlinWasmWasiTargetDsl> = {}): KotlinWasmWasiTargetDsl? {
        return if (name in allowed) kotlin.wasmWasi(name, configure) else null
    }

}

val Project.enabledTargets: List<KotlinTarget>
    get() {
    return extensions.getByType(KotlinMultiplatformExtension::class.java).targets.filterNotNull().toList()
}

internal val androidNativeTargets = listOf(
    "androidNativeX86",
    "androidNativeX64",
    "androidNativeArm32",
    "androidNativeArm64"
)

internal val linuxTargets = listOf(
    "linuxX64",
    "linuxArm64"
)

internal val macOsTargets = listOf(
    "macosX64",
    "macosArm64"
)

private val appleTargets = macOsTargets + listOf(
    "iosSimulatorArm64",
    "iosX64",
    "iosArm64",
    "tvosX64",
    "tvosArm64",
    "tvosSimulatorArm64",
    "watchosX64",
    "watchosArm32",
    "watchosArm64",
    "watchosSimulatorArm64",
    "watchosDeviceArm64"
)

internal val windowsTargets = listOf(
    "mingwX64"
)

private val webTargets = listOf(
    "warmJs",
    "js",
    "wasmWasi"
)

val Project.allowedTargets get() = run {
    getKniProperty(Config.ARG_ALLOWED_TARGETS)?.split(',')
        ?: (linuxTargets + appleTargets + windowsTargets + androidNativeTargets + webTargets)
}