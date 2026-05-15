plugins {
    alias(libs.plugins.kni)
}

kotlin {
    val linux64 = linuxX64 {
        binaries.sharedLib() // define what you need here, like cinterops
    }
    val androidNative = androidNativeArm64 {
        binaries.sharedLib()
    }

    // linuxX64 .so artifact will
    // be added to lib/ directory inside the JAR.
    jvm() bundlesNatives listOf(linux64)

    android {
        // androidNativeArm64 .so artifact will
        // be added to jniLibs inside the AAR.
        bundlesNatives(listOf(androidNative))
    }
}