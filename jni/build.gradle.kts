@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.konan.target.Family

import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.osdetector)
    alias(libs.plugins.ksp)
    alias(libs.plugins.test)
    alias(libs.plugins.android.library)
    alias(libs.plugins.publish)
    alias(libs.plugins.dokka)
    id("com.dshatz.kni")
    `maven-publish`
    signing
}

val libGroup = VersionCatalog.artifactName()
val libName = "jni"
group = libGroup
version = libVersion

kotlin {
    jvmToolchain(21)

    optionalTargets {
        androidNativeX86 {
            binaries {
                sharedLib()
                staticLib()
            }
        }
        androidNativeX64 {
            binaries {
                sharedLib {
                    linkerOpts += listOf(
                        "-Wl,-z,max-page-size=16384",
                        "-Wl,-z,common-page-size=16384",
                        "-v"
                    )
                }
                staticLib {
                    linkerOpts += listOf(
                        "-Wl,-z,max-page-size=16384",
                        "-Wl,-z,common-page-size=16384",
                        "-v"
                    )
                }
            }
        }
        androidNativeArm32 {
            binaries {
                sharedLib()
                staticLib()
            }
        }
        androidNativeArm64 {
            binaries {
                sharedLib()
                staticLib()
            }
        }
        val desktopTargets = listOfNotNull(
            linuxX64 {},
            linuxArm64(),
            mingwX64(),
            macosX64(),
            macosArm64()
        )

        desktopTargets.forEach { target ->
            target.binaries {
                sharedLib()
                staticLib()
            }
            target.compilations.getByName("main") {
                cinterops {
                    create("jni") {
                        val osFolder = when {
                            target.konanTarget.family.isAppleFamily -> "darwin"
                            target.konanTarget.family == Family.LINUX -> "linux"
                            target.konanTarget.family == Family.MINGW -> "win32"
                            else -> null
                        }

                        val jniHeadersBase = project.file("jni-headers")
                        includeDirs.allHeaders(jniHeadersBase)
                        osFolder?.let { includeDirs.allHeaders(jniHeadersBase.resolve(it)) }
                    }
                }
            }
        }
    }

    jvm()
    androidLibrary {
        namespace = "$libGroup.$libName"
        compileSdk = 36
    }

    applyDefaultHierarchyTemplate {
        common {
            group("native") {
                group("desktopNative") {
                    withLinux()
                    withMacos()
                    withMingw()
                }
                group("androidNative") {
                    withAndroidNative()
                }
            }
            group("androidJvm") {
                withAndroidTarget()
                withJvm()
            }
        }
    }

    sourceSets {
        val androidMain by getting

        val androidJvmMain by getting
        androidMain.dependsOn(androidJvmMain)
        commonTest.dependencies {
            implementation(libs.test)
        }
        nativeTest.dependencies {
            implementation(libs.kotest)
            implementation(libs.test.kotest)
        }
        nativeMain.dependencies {
            implementation(project(":buffers"))
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
        description.set("High-performance common ByteBuffer for Kotlin/Native.")
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
    reports {
        junitXml.required.set(true)
    }
}

tasks.withType<KotlinNativeTest>().configureEach {
    outputs.upToDateWhen { false }
}