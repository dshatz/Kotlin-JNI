import org.gradle.api.Project

class VersionCatalog() {
    companion object {
        fun artifactName(module: String = ""): String {
            return if (module.isBlank()) {
                Configuration.packageName
            } else {
                "${Configuration.packageName}.$module"
            }
        }
    }
}

val Project.libVersion: String
    get() = project.property("version").toString()