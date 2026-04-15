plugins {
    `java-gradle-plugin`
    alias(libs.plugins.jvm)
}

group = "com.dshatz.kni"
version = project.findProperty("version") as? String ?: "0.1.0-SNAPSHOT1"

gradlePlugin {
    val kotlinJni by plugins.creating {
        id = "com.dshatz.kni"
        implementationClass = "com.dshatz.kni.Plugin"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
    implementation("com.android.kotlin.multiplatform.library:com.android.kotlin.multiplatform.library.gradle.plugin:8.13.2")
}