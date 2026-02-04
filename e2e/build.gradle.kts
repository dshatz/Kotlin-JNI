@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.test)
    alias(libs.plugins.ksp)
    alias(libs.plugins.osdetector)
    alias(libs.plugins.android.library)
}

val packageAndroidNatives = tasks.register<Copy>("packageAndroidNatives") {
    group = "build"
    description = "Aggregates all native libs for Android packaging."
    val outputDir = project.file("src/androidMain/jniLibs")
    into(outputDir)
}

kotlin {

    applyDefaultHierarchyTemplate()

    jvmToolchain(21)
    androidLibrary {
        namespace = "dev.datlag.nkommons.e2e"
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
    }
    jvm()

    fun KotlinNativeTargetWithHostTests.setupTestLib() {
        binaries.sharedLib()
        binaries.withType<SharedLibrary> {
            if (this.buildType == NativeBuildType.DEBUG) {
                val linkTask = linkTaskProvider
                tasks.withType<Test>().configureEach {
                    dependsOn(linkTask)
                    systemProperty("java.library.path", linkTask.get().destinationDirectory.get().asFile.absolutePath)
                }
            }
        }
    }

    if (getHost() == Host.Linux) {
        linuxX64 {
            setupTestLib()
        }
    }
    if (getHost() == Host.MAC) {
        macosArm64 {
            setupTestLib()
        }
    }
    if (getHost() == Host.Windows) {
        mingwX64 {
            setupTestLib()
        }
    }

    val androidArchMap = mapOf(
        "androidNativeArm64" to "android/arm64-v8a",
        "androidNativeX64"   to "android/x86_64",
        "androidNativeArm32" to "android/armeabi-v7a",
        "androidNativeX86"   to "android/x86"
    )

    androidNativeX64 {
        binaries.sharedLib()
    }


    androidNativeArm64 {
        val target = this
        binaries.sharedLib()
        binaries.withType<TestExecutable>().configureEach {
            val binary = binaries.getSharedLib(NativeBuildType.DEBUG)
            packageAndroidNatives.configure {
                dependsOn(binary.linkTaskProvider)
                from(binary.outputFile) {
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                    val folder = androidArchMap[target.name]!!.substringAfter("android/")
                    into(folder)
                }
            }
        }
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


afterEvaluate {
    tasks.named("mergeAndroidMainJniLibFolders").configure {
        dependsOn(packageAndroidNatives)
    }
}
