import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.publish)
    alias(libs.plugins.test)
    `maven-publish`
    signing
}

val libGroup = VersionCatalog.artifactName()
val libName = "buffers"
group = libGroup
version = libVersion

kotlin {
    jvmToolchain(21)
    androidLibrary {
        compileSdk = Configuration.compileSdk
        minSdk = Configuration.minSdk

        namespace = "$libGroup.$libName"
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

    androidNativeX64()
    androidNativeArm64()
    androidNativeX86()
    androidNativeArm32()

    linuxX64 {
        setupTestLib()
    }
    linuxArm64()

    mingwX64 {
        setupTestLib()
    }

    macosX64 {
        setupTestLib()
    }
    macosArm64 {
        setupTestLib()
    }

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

    if (project.hasProperty("enableWeb")) {
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

    applyDefaultHierarchyTemplate {
        common {
            group("androidJvm") {
                withAndroidTarget()
                withJvm()
            }
        }
    }

    sourceSets {
        val androidJvmMain by getting
        val androidMain by getting {
            dependsOn(androidJvmMain)
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.test.core)
                implementation(libs.test.kotest)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.coroutines.core)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.junit)
            }
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
    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }
    reports {
        junitXml.required.set(true)
    }
}

tasks.withType<KotlinNativeTest>().configureEach {
    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }
    reports {
        junitXml.required.set(true)
    }
}

tasks.withType<KotlinTest>().configureEach {
    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }
}