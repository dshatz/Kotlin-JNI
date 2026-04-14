@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.dshatz.kni.bundlesNatives
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable

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

    fun KotlinNativeTargetWithHostTests.setupTestLib() {
        binaries.sharedLib()
        /*binaries.withType<SharedLibrary> {
            if (this.buildType == NativeBuildType.DEBUG) {
                val linkTask = linkTaskProvider
                tasks.withType<Test>().configureEach {
                    dependsOn(linkTask)
                    systemProperty("java.library.path", linkTask.get().destinationDirectory.get().asFile.absolutePath)
                }
            }
        }*/
    }

    val desktopNativeTargets = buildList {
        if (getHost() == Host.Linux) {
            add(linuxX64 {
                setupTestLib()
            })
        }

        if (getHost() == Host.MAC) {
            add(macosArm64 {
                setupTestLib()
            })
            add(macosX64 {
                setupTestLib()
            })
        }

        if (getHost() == Host.Windows) {
            add(mingwX64 {
                setupTestLib()
            })
        }
    }

    val androidNativeTargets = listOf(
        androidNativeX64 {
            binaries.sharedLib()
        },
        androidNativeArm64 {
            binaries.sharedLib()
        }
    )


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

        jvmTest.dependencies {
            implementation(libs.kotest.junit)
        }
        nativeMain.dependencies {
            implementation(libs.coroutines.core)
        }
        commonMain.dependencies {
            implementation(project(":buffers"))
        }
    }
}

tasks.withType<Test>().configureEach {
//    useJUnitPlatform()
    reports {
        junitXml.required.set(true)
    }
}

dependencies {
    if (getHost() == Host.Linux) {
        add("kspLinuxX64", project(":ksp"))
    }
    if (getHost() == Host.MAC) {
        add("kspMacosArm64", project(":ksp"))
    }
    if (getHost() == Host.Windows) {
        add("kspMingwX64", project(":ksp"))
    }
    add("kspAndroidNativeX64", project(":ksp"))
    add("kspAndroidNativeArm64", project(":ksp"))
}

fun getHost(): Host {
    return when (osdetector.os) {
        "linux" -> Host.Linux
        "osx" -> Host.MAC
        "windows" -> Host.Windows
        else -> {
            val hostOs = System.getProperty("os.name")
            val isMingwX64 = hostOs.startsWith("Windows")

            when {
                hostOs == "Linux" -> Host.Linux
                hostOs == "Mac OS X" -> Host.MAC
                isMingwX64 -> Host.Windows
                else -> throw IllegalStateException("Unknown OS: ${osdetector.classifier}")
            }
        }
    }
}

enum class Host(val label: String) {
    Linux("linux"),
    Windows("win"),
    MAC("mac");
}

tasks.withType<Test>().configureEach {
    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }
}