plugins {
    id("vista.android.library")
    id("vista.hilt")
    alias(libs.plugins.ksp)
}

android {
    namespace = "ir.coffevista.vista_native.core.worker"
}

dependencies {
    implementation(libs.androidx.work.runtime)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.work.testing)
}
