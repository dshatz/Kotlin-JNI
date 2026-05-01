plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.multiplatform) apply  false
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.osdetector) apply false

    alias(libs.plugins.test) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.dokka)
}

dependencies {
    dokka(project(":jni"))
    dokka(project(":buffers"))
}