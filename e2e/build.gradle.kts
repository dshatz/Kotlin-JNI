@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

import com.dshatz.kni.bundlesNatives
import com.dshatz.kni.gettingOptional
import com.google.devtools.ksp.gradle.KspAATask
import org.gradle.kotlin.dsl.withType
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
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
        createSourceSets = false
    }
}

fun KotlinNativeTarget.androidLinkerOpts() {
    binaries.all {
        // Force the linker to use 16KB alignment
        linkerOpts("-z", "max-page-size=16384")
        linkerOpts("-z", "common-page-size=16384")
        linkerOpts("-Wl,--allow-shlib-undefined")
    }
}

kotlin {

    applyHierarchyTemplate {
        common {
            group("androidNative") {
                withAndroidNative()
            }
            group("skikoNative") {
                withLinux()
                withMacos()
            }
            group("desktopNative") {
                group("mingw") {
                    withMingw()
                }
                group("skikoNative")
            }
            group("android") {
                withAndroidTarget()
            }
            group("jniJvm") {
                group("android")
                withJvm()
            }
            group("jniNative") {
                group("androidNative")
                group("desktopNative")
            }
            group("jniCommon") {
                group("jniNative")
                group("jniJvm")
            }
        }
    }

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
    androidNativeTargets.forEach {
        it.binaries.sharedLib()
        it.androidLinkerOpts()
    }

    optionalTargets {
        wasmJs {
            binaries.executable()
            browser()
        }
    }


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
            }
        }
        val jniCommonMain = getByName("jniCommonMain") {
            dependencies {
                implementation(project(":jni"))
            }
        }
        val androidDeviceTest = getByName("androidDeviceTest") {
            dependsOn(getByName("jniJvmTest"))
        }

        val jniJvmMain = getByName("jniJvmMain")

        androidMain.configure {
            dependsOn(jniJvmMain)
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
            implementation(project(":e2e-adapters"))
            implementation(project(":flows"))
            implementation(project(":adapters"))
        }
        val skikoNativeMain by gettingOptional {
            dependencies {
                implementation(libs.skiko)
            }
        }
        mingwMain.dependencies {
            implementation(libs.skiko.native)
        }
        jvmMain.dependencies {
            implementation(libs.skiko)
            val currentOs = DefaultNativePlatform.getCurrentOperatingSystem()
            if (currentOs.isWindows) {
                implementation(libs.skiko.runtime.mingwX64)
            } else if (currentOs.isMacOsX) {
                implementation(libs.skiko.runtime.macosArm64)
                implementation(libs.skiko.runtime.macosX64)
            } else if (currentOs.isLinux) {
                implementation(libs.skiko.runtime.linuxArm64)
                implementation(libs.skiko.runtime.linuxX64)
            }
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