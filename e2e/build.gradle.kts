@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.dshatz.kni.bundlesNatives
import com.google.devtools.ksp.gradle.KspAATask
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.test)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.library)
    id("com.dshatz.kni")
}

kni {
    autoWire {
        kspDependency = project(":ksp")
    }
}

kotlin {

    applyDefaultHierarchyTemplate()

    jvmToolchain(21)

    val desktopNativeTargets = optionalTargets.run {
        listOfNotNull(
            linuxX64(),
            linuxArm64(),
            macosArm64(),
            macosX64(),
            mingwX64()
        )

    }

    val androidNativeTargets = optionalTargets.run {
        listOfNotNull(
            androidNativeX64(),
            androidNativeArm64()
        )
    }
    desktopNativeTargets.forEach { it.binaries.sharedLib() }
    androidNativeTargets.forEach { it.binaries.sharedLib() }


    jvm {
        bundlesNatives(desktopNativeTargets)
    }

    android {
        namespace = "com.dshatz.kni.e2e"
        compileSdk = 36
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            this.instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        packaging {
            resources.excludes.add("META-INF/AL2.0")
            resources.excludes.add("META-INF/LGPL2.1")
        }
        minSdk = 26
        bundlesNatives(androidNativeTargets)
    }



    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotest)
            implementation(libs.kotlin.reflect)
            implementation(libs.coroutines.test)
            implementation(libs.test.core)
            implementation(libs.test.kotest)
        }

        commonMain {
            dependencies {
                implementation(project(":annotations"))
                implementation(project(":serialization"))
                implementation(project(":jni"))
            }
        }
        val androidDeviceTest by getting

        val jniJvmMain by getting {
            println("Got jniJvmMain")
        }

        androidDeviceTest.dependencies {
            implementation(libs.android.runner)
            implementation(libs.test.core)
            implementation(libs.junit4)
        }

        nativeMain.dependencies {
            implementation(libs.coroutines.core)
        }
        commonMain.dependencies {
            implementation(project(":buffers"))
            implementation(project(":flows"))
        }
    }
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.withType<Test>().configureEach {
    outputs.upToDateWhen { false }
    failOnNoDiscoveredTests = false
}

tasks.withType<KotlinNativeTest>().configureEach {
    outputs.upToDateWhen { false }
    failOnNoDiscoveredTests = false
}

tasks.withType<KotlinTest>().configureEach {
    outputs.upToDateWhen { false }
    failOnNoDiscoveredTests = false
}