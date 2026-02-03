import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog as VCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension as VCatalogEx
import org.gradle.kotlin.dsl.getByType

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