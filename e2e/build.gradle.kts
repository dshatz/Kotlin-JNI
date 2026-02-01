import io.kotest.framework.gradle.tasks.KotestJvmTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
    alias(libs.plugins.osdetector)
    alias(libs.plugins.android.library)
}

kotlin {

    jvmToolchain(21)
    androidLibrary {
        namespace = "dev.datlag.nkommons.e2e"
        compileSdk = 36
    }
    jvm()

    fun KotlinNativeTargetWithHostTests.setupTestLib() {
        binaries.sharedLib()
        binaries.withType<SharedLibrary> {
            val linkTask = linkTaskProvider
            tasks.withType<Test>().configureEach {
                dependsOn(linkTask)
                systemProperty("java.library.path", linkTask.get().destinationDirectory.get().asFile.absolutePath)
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

    sourceSets {
        commonMain.dependencies {
            implementation(project(":annotations"))
        }
        commonTest.dependencies {
            implementation(libs.bundles.kotest)
            implementation(libs.kotlin.reflect)
            implementation(libs.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.junit)
        }
        nativeMain.dependencies {
            implementation(project(":jni"))
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    ksp(project(":ksp"))
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