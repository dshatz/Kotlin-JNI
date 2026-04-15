import com.dshatz.kni.bundlesNatives
import com.dshatz.kni.bundlesPrebuiltNatives
import com.dshatz.kni.addKspForNative

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.library)
    alias(libs.plugins.test)
    id("com.dshatz.kni")
}

kotlin {
    jvmToolchain(21)
    applyDefaultHierarchyTemplate()

    val androidNatives = optionalTargets.run {
        listOfNotNull(
            androidNativeX64 {
                binaries.sharedLib()
            },
            androidNativeArm64 {
                binaries.sharedLib()
            }
        )
    }
    androidLibrary {
        namespace = "com.dshatz.kni.plugintest"
        compileSdk = 36
        minSdk = 26
        bundlesNatives(androidNatives)
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            this.instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        bundlesPrebuiltNatives {
            all.add(project.layout.projectDirectory.dir("prebuilt/android-all"))
        }
    }

    val desktopTargets = optionalTargets.run {
        listOfNotNull(
            linuxX64 {
                binaries.sharedLib {

                }
            },
            linuxArm64 {
                binaries.sharedLib {

                }
            }
        )
    }
    jvm {
        this.mainRun {
            mainClass = "com.dshatz.kni.plugintest.MainKt"
        }
        this.binaries {
            this.executable {
                this.mainClass = "com.dshatz.kni.plugintest.MainKt"
            }
        }
        this bundlesNatives desktopTargets
        bundlesPrebuiltNatives {
            linuxX64.add(project.layout.projectDirectory.dir("prebuilt/linuxX64"))
            linuxArm64.add(project.layout.projectDirectory.dir("prebuilt/linuxArm64"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":annotations"))
            implementation(project(":jni"))
        }

        commonTest.dependencies {
            implementation(libs.test.core)
            implementation(libs.test.kotest)
        }
        val nonNativeTest by creating {
            dependsOn(commonTest.get())
        }
        val nonNativeMain by creating {
            dependsOn(commonMain.get())
        }
        jvmMain.configure {
            dependsOn(nonNativeMain)
        }
        jvmTest.configure {
            dependsOn(nonNativeTest)
        }
        androidMain.configure {
            dependsOn(nonNativeMain)
        }
        named("androidDeviceTest") {
            dependsOn(getByName("nonNativeTest"))
            dependencies {
                implementation(libs.test.core)
                implementation("androidx.test:runner:1.7.0")
            }
        }
    }
}

dependencies {
    addKspForNative(project(":ksp"))
}

tasks.named<Jar>("jvmJar") {
    manifest {
        attributes["Main-Class"] = "com.dshatz.kni.plugintest.MainKt"
    }
}
