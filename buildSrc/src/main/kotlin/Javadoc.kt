import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar

/**
 * Publish empty javadoc jars. The docs are hosted on GitHub Pages and IDEs read KDoc from the sources jars.
 *
 * Maven Central requires a javadoc jar for every publication but allows it to be empty. Without this, the publish
 * plugin packs the full Dokka output into the javadoc jar of every publication (jvm, mingwX64, ...), which made
 * the javadoc jars ~90% of a release.
 */
fun Project.emptyJavadocJars() {
    tasks.withType(Jar::class.java).configureEach {
        if (name.endsWith("DokkaJavadocJar")) {
            exclude("**")
        }
    }
}
