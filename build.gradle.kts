plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.dependency.check)
}

if (providers.gradleProperty("vista.isolatedBuildDir").orNull == "true") {
    val isolatedBuildOutput = providers.gradleProperty("vista.isolatedBuildOutput")
        .orElse("fnd-build-output")
    allprojects {
        val modulePath = if (path == ":") {
            "root"
        } else {
            path.trimStart(':').replace(':', '/')
        }
        layout.buildDirectory.set(
            rootProject.layout.projectDirectory.dir(
                ".gradle/${isolatedBuildOutput.get()}/$modulePath",
            ),
        )
    }
}

dependencyCheck {
    failBuildOnCVSS = 7.0F
    formats = listOf("HTML", "JSON")
    outputDirectory.set(layout.buildDirectory.dir("reports/dependency-check"))
    nvd.apiKey = providers.environmentVariable("NVD_API_KEY").orNull
    analyzers.ossIndex.enabled = false
}
