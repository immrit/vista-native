plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.dependency.check)
}

dependencyCheck {
    failBuildOnCVSS = 7.0F
    formats = listOf("HTML", "JSON")
    outputDirectory.set(layout.buildDirectory.dir("reports/dependency-check"))
    nvd.apiKey = providers.environmentVariable("NVD_API_KEY").orNull
    analyzers.ossIndex.enabled = false
}
