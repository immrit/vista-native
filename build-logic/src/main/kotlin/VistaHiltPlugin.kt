import org.gradle.api.Plugin
import org.gradle.api.Project

class VistaHiltPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.dagger.hilt.android")
    }
}
