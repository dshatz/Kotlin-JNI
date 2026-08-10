package com.dshatz.kni.load

import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.io.path.createTempDirectory

actual object BundledLibLoader {
    val loaded: MutableSet<String> = mutableSetOf()
    actual fun loadBundledLibrary(name: String) {
        if (name in loaded) return
        loadLibraryFromJar(name)
        loaded += name
    }

    private val osName = System.getProperty("os.name").lowercase(Locale.ENGLISH)
    private val osArch = System.getProperty("os.arch").lowercase(Locale.ENGLISH)

    private fun loadLibraryFromJar(baseName: String) {
        val (platformDir, extension, prefix) = getPlatformDetails()

        val fileName = "${prefix}${baseName}.${extension}"

        val resourcePath = "lib/$platformDir/$fileName"

        val tmpDir = createTempDirectory("${baseName}_libs").toFile()
        tmpDir.deleteOnExit()
        val tmpFile = File(tmpDir, fileName)

        val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
            ?: throw UnsatisfiedLinkError("Native library not found in JAR at: $resourcePath")

        FileOutputStream(tmpFile).use { out ->
            resourceStream.use { it.copyTo(out) }
        }

        try {
            System.load(tmpFile.absolutePath)
        } catch (e: UnsatisfiedLinkError) {
            System.err.println("Failed to load library: ${tmpFile.absolutePath}: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private data class PlatformInfo(val dir: String, val ext: String, val prefix: String)

    private fun getPlatformDetails(): PlatformInfo {
        val isArm64 = osArch.contains("aarch64") || osArch.contains("arm64")
        val isX64 = osArch.contains("x86_64") || osArch.contains("amd64")

        if (osName.contains("win")) {
            // Windows: uses .dll, no "lib" prefix
            return PlatformInfo("mingwX64", "dll", "")
        }
        else if (osName.contains("mac")) {
            // Mac: uses .dylib, "lib" prefix
            val arch = if (isArm64) "Arm64" else "X64"
            return PlatformInfo("macos$arch", "dylib", "lib")
        }
        else if (osName.contains("nux") || osName.contains("nix")) {
            // Linux: uses .so, "lib" prefix
            // Check for ARM vs X64
            val arch = if (isArm64) "Arm64" else "X64"
            return PlatformInfo("linux$arch", "so", "lib")
        }

        throw UnsupportedOperationException("Unsupported OS/Arch: $osName / $osArch")
    }
}