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

plugins {
    id("com.gradle.develocity") version "4.3.2"
}

develocity {
    buildScan {
        termsOfUseAgree.set("yes")
        termsOfUseUrl = "https://gradle.com/terms-of-service"

//        publishing.onlyIf { System.getenv("GITHUB_ACTIONS") == "true" }
    }
}
