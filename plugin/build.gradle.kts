plugins {
    `java-gradle-plugin`
    alias(libs.plugins.jvm)
    alias(libs.plugins.publish)
    signing
}

val libGroup = "com.dshatz.kni"
group = libGroup
val libName = "plugin"
val libVersion = project.findProperty("version") as? String ?: "0.1.0-SNAPSHOT1"
version = libVersion

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

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = libGroup,
        artifactId = libName,
        version = libVersion
    )

    pom {
        name.set("Kotlin-JNI")
        description.set("Painless JNI with Kotlin/Native using a KSP processor.")
        url.set("https://github.com/dshatz/Kotlin-JNI")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        scm {
            url.set("https://github.com/dshatz/Kotlin-JNI")
            connection.set("scm:git:git://github.com/dshatz/Kotlin-JNI")
            developerConnection.set("scm:git:git://github.com/dshatz/Kotlin-JNI.git")
        }

        developers {
            developer {
                id.set("dshatz")
                name.set("Daniels Šatcs")
                url.set("https://github.com/dshatz")
            }
            developer {
                id.set("DatL4g")
                name.set("Jeff Retz")
                url.set("https://github.com/DatL4g")
            }
        }
    }
}