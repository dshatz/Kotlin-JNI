@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.dshatz.kni.bundlesNatives
import com.dshatz.kni.addKspForNative
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.test)
    alias(libs.plugins.ksp)
    id("com.dshatz.kni")
    alias(libs.plugins.osdetector)
    alias(libs.plugins.android.library)
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

    androidLibrary {
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

        val commonMain by getting {
            dependencies {
                implementation(project(":annotations"))
                implementation(project(":jni"))
            }
        }
        val androidDeviceTest by getting

        val androidJvmMain by creating {
            dependsOn(commonMain)
        }
        val androidMain by getting {
            dependsOn(androidJvmMain)
        }
        val jvmMain by getting


        val androidJvmTest by creating {
            dependsOn(commonTest.get())
        }

        val jvmTest by getting {
            dependsOn(androidJvmTest)
        }

        androidDeviceTest.dependencies {
            implementation(libs.android.runner)
            implementation(libs.test.core)
            implementation(libs.junit4)
        }
        androidDeviceTest.dependsOn(androidJvmTest)

        nativeMain.dependencies {
            implementation(libs.coroutines.core)
        }
        commonMain.dependencies {
            implementation(project(":buffers"))
        }
    }
}

tasks.withType<Test>().configureEach {
    reports {
        junitXml.required.set(true)
    }
}

dependencies {
    addKspForNative(project(":ksp"))
}