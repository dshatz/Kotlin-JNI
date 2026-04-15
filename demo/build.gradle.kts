import com.dshatz.kni.addKspForNative

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.osdetector)
    alias(libs.plugins.ksp)
    id("com.dshatz.kni")
}

kotlin {
    jvmToolchain(21)
    jvm {
        mainRun {
            mainClass = "com.dshatz.kni.MainKt"
        }
    }

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
        desktopTargets.forEach {
            it.binaries.sharedLib()
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":annotations"))
            implementation(project(":jni"))
            implementation(project(":buffers"))
        }
    }
}

dependencies {
    addKspForNative(project(":ksp"))
}