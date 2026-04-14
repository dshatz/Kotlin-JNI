pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    plugins {
        kotlin("jvm").version("2.1.21") apply false
        kotlin("plugin.serialization") version "2.1.21" apply false
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}