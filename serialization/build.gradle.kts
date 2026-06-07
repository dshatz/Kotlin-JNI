@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    id("com.dshatz.kni")
    alias(libs.plugins.publish)
    `maven-publish`
    signing
}

kni {
    autoWire {}
}

val libGroup = VersionCatalog.artifactName()
val libName = "serialization"
group = libGroup
version = libVersion

kotlin {
    jvmToolchain(21)
    applyDefaultHierarchyTemplate()

    jvm()
    android {
        compileSdk = Configuration.compileSdk
        minSdk = Configuration.minSdk

        namespace = "$libGroup.$libName"
    }

    optionalTargets {
        androidNativeX64()
        androidNativeArm64()
        androidNativeX86()
        androidNativeArm32()


        linuxX64()
        linuxArm64()
        mingwX64()
        macosX64()
        macosArm64()

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
            browser()
            nodejs()
        }
        wasmJs {
            browser()
            nodejs()
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.io)
        }
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