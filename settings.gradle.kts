rootProject.name = "Kotlin-JNI"

include(":annotations")
include(":jni")
include(":ksp")
include(":demo")
include(":e2e")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
