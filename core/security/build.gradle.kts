plugins {
    id("vista.android.library")
    id("vista.hilt")
    alias(libs.plugins.ksp)
}

android {
    namespace = "ir.coffevista.vista_native.core.security"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(libs.hilt.android)
    api(libs.androidx.biometric)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.json.jvm)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
