import org.jetbrains.kotlin.konan.target.Family
import java.net.URI
import java.net.URL

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
    jvmToolchain(21)
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