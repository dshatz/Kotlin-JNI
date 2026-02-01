plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.publish)
    `maven-publish`
    signing
}

val libGroup = VersionCatalog.artifactName()
val libName = "ksp"
group = libGroup
version = libVersion

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":annotations"))
        }

        commonTest.dependencies {
            implementation(libs.test)
        }

        jvmMain.dependencies {
            implementation(libs.ksp)
            implementation(libs.kotlinpoet)
            implementation(libs.kotlinpoet.ksp)
        }
    }
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
        name.set("Kotlin-JNI KSP")
        description.set("KSP Generator for Kotlin-JNI.")
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