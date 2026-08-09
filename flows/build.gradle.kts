@file:OptIn(ExperimentalWasmDsl::class, ExperimentalKotlinGradlePluginApi::class)

import com.dshatz.kni.gettingOptional
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.publish)
    alias(libs.plugins.test)
    alias(libs.plugins.dokka)
    id("com.dshatz.kni")
    `maven-publish`
    signing
}

val libGroup = VersionCatalog.artifactName()
val libName = "flows"
group = libGroup
version = libVersion

kni {
    autoWire {
        kspDependency = project(":ksp")
    }
}

kotlin {
    jvmToolchain(21)
    android {
        compileSdk = Configuration.compileSdk
        minSdk = Configuration.minSdk

        namespace = "$libGroup.$libName"

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            this.instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    applyHierarchyTemplate {
        common {
            group("androidNative") {
                withAndroidNative()
            }
            group("jniJvm") {
                withAndroidTarget()
                withJvm()
            }
            group("jniNative") {
                withLinux()
                withMacos()
                withMingw()
                group("androidNative")
            }
            group("jniCommon") {
                group("jniJvm")
                group("jniNative")
            }
            group("nonJni") {
                withIos()
                withTvos()
                withWatchos()
                group("web") {
                    withWasmJs()
                    withJs()
                }
            }
        }
    }

    jvm()

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

        iosX64()
        iosArm64()
        iosSimulatorArm64()

        tvosX64()
        tvosArm64()
        tvosSimulatorArm64()

        watchosX64()
        watchosArm32()
        watchosArm64()
        watchosSimulatorArm64()
        watchosDeviceArm64()

        js {
            browser {
                testTask {
                    useKarma {
                        useFirefoxHeadless()
                    }
                }
            }
            nodejs()
        }
        wasmJs {
            browser {
                testTask {
                    useKarma {
                        useFirefoxHeadless()
                    }
                }
            }
            nodejs()
        }
    }


    applyDefaultHierarchyTemplate()

    sourceSets {
        val androidDeviceTest by getting

        val jsMain by gettingOptional {
            dependencies {
                implementation(libs.coroutines.core)
            }
        }
        commonMain.dependencies {
            api(libs.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.test.core)
            implementation(libs.test.kotest)
        }

        val jniCommonMain by getting {
            dependencies {
                implementation(project(":annotations"))
            }
        }

        androidDeviceTest.dependencies {
            implementation(libs.android.runner)
            implementation(libs.test.core)
            implementation(libs.junit4)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = libGroup,
        artifactId = libName,
        version = libVersion
    )

    pom {
        name.set("Kotlin-JNI")
        description.set("Painless JNI with Kotlin/Native using a KSP processor.")
        url.set("https://github.com/dshatz/Kotlin-JNI")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        scm {
            url.set("https://github.com/dshatz/Kotlin-JNI")
            connection.set("scm:git:git://github.com/dshatz/Kotlin-JNI")
            developerConnection.set("scm:git:git://github.com/dshatz/Kotlin-JNI.git")
        }

        developers {
            developer {
                id.set("dshatz")
                name.set("Daniels Šatcs")
                url.set("https://github.com/dshatz")
            }
            developer {
                id.set("DatL4g")
                name.set("Jeff Retz")
                url.set("https://github.com/DatL4g")
            }
        }
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

dokka {
    dokkaSourceSets.configureEach {
        externalDocumentationLinks.register("kotlinx-io") {
            url("https://kotlinlang.org/api/kotlinx-io/kotlinx-io-core/")
            packageListUrl("https://kotlinlang.org/api/kotlinx-io/package-list")
        }
    }
}
