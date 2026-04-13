rootProject.name = "Kotlin-JNI"

if (!settings.extra.has("onlyDocs")) {
    include(":annotations")
    include(":demo")
    include(":e2e")
    include(":ksp")
}
include(":jni")
include(":buffers")

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
    }
}
