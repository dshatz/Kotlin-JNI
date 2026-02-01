plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.osdetector)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
    alias(libs.plugins.publish)
    `maven-publish`
    signing
}

val libGroup = VersionCatalog.artifactName()
val libName = "jni"
group = libGroup
version = libVersion

kotlin {
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

    val desktopTargets = mutableListOf(
        linuxX64 {
            binaries {
                sharedLib()
                staticLib()
            }
        },
        linuxArm64 {
            binaries {
                sharedLib()
                staticLib()
            }
        },
        mingwX64 {
            binaries {
                sharedLib()
                staticLib()
            }
        }
    )

    if (getHost() == Host.MAC) {
        desktopTargets.add(
            macosX64 {
                binaries {
                    sharedLib()
                    staticLib()
                }
            }
        )
        desktopTargets.add(
            macosArm64 {
                binaries {
                    sharedLib()
                    staticLib()
                }
            }
        )
    }

    desktopTargets.forEach { target ->
        target.compilations.getByName("main") {
            cinterops {
                create("jni") {
                    val javaDefaultHome = System.getProperty("java.home")
                    val javaEnvHome = getSystemJavaHome()
                    val javaSdkMan = getSdkManJava()
                    val javaDefaultRuntime = getDefaultRuntimeJava()
                    val javaFallbackRuntime = getFallbackRuntimeJava()

                    includeDirs.allHeaders(
                        // Gradle or IDE specified Java Home
                        File(javaDefaultHome, "include"),
                        File(javaDefaultHome, "include/darwin"),
                        File(javaDefaultHome, "include/linux"),
                        File(javaDefaultHome, "include/win32"),

                        // System set Java Home
                        File(javaEnvHome, "include"),
                        File(javaEnvHome, "include/darwin"),
                        File(javaEnvHome, "include/linux"),
                        File(javaEnvHome, "include/win32"),

                        // SDK Man Java
                        File(javaSdkMan, "include"),
                        File(javaSdkMan, "include/darwin"),
                        File(javaSdkMan, "include/linux"),
                        File(javaSdkMan, "include/win32"),

                        // Default Runtime Java
                        File(javaDefaultRuntime, "include"),
                        File(javaDefaultRuntime, "include/darwin"),
                        File(javaDefaultRuntime, "include/linux"),
                        File(javaDefaultRuntime, "include/win32"),

                        // Fallback Runtime Java
                        File(javaFallbackRuntime, "include"),
                        File(javaFallbackRuntime, "include/darwin"),
                        File(javaFallbackRuntime, "include/linux"),
                        File(javaFallbackRuntime, "include/win32"),
                    )
                }
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        nativeTest.dependencies {
            implementation(libs.bundles.kotest)
        }

        val desktopNativeMain by creating {
            dependsOn(nativeMain.get())

            linuxMain.orNull?.dependsOn(this)
            mingwMain.orNull?.dependsOn(this)

            macosMain.orNull?.dependsOn(this)
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

fun getSystemJavaHome(): String? {
    return System.getenv("JAVA_HOME")?.ifBlank { null }
}

fun getSdkManJava(): String? {
    return System.getProperty("user.home")?.ifBlank { null }?.let {
        "$it/.sdkman/candidates/java/current"
    }
}

fun getDefaultRuntimeJava(): String? {
    return "/usr/lib/jvm/default-runtime"
}

fun getFallbackRuntimeJava(): String? {
    return "/usr/lib/jvm/java"
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