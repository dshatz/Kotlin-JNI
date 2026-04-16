@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.dshatz.kni.bundlesNatives
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.osdetector)
    alias(libs.plugins.ksp)
    id("com.dshatz.kni")
}

kni {
    autoWire {
        kspDependency = project(":ksp")
    }
}

kotlin {
    jvmToolchain(21)

    optionalTargets {
        androidNativeX64()
        androidNativeArm64()
        androidNativeX86()
        androidNativeArm32()

        val desktopTargets = listOfNotNull(
            linuxX64(),
            linuxArm64(),
            mingwX64(),
            macosX64(),
            macosArm64()
        )
        desktopTargets.forEach {
            it.binaries.sharedLib()
        }

        jvm {
            mainRun {
                mainClass = "com.dshatz.kni.MainKt"
            }
            bundlesNatives(desktopTargets)
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":annotations"))
            implementation(project(":jni"))
            implementation(project(":buffers"))
        }
    }
}