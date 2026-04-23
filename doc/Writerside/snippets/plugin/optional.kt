plugins {
    alias(libs.plugins.kni)
}

kotlin {
    optionalTargets {
        androidNativeX64()
        androidNativeArm64()
        androidNativeX86()
        androidNativeArm32()

        linuxX64 {
            binaries.sharedLib()
        }
        linuxArm64()
        mingwX64()
        macosX64()
        macosArm64()

        iosX64()
        iosArm64()
        iosSimulatorArm64()

        js()
        wasmJs()
        wasmWasi()
        // other targets
    }
}