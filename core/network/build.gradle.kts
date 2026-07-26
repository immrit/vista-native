plugins {
    id("vista.android.library")
    id("vista.hilt")
    alias(libs.plugins.ksp)
}

android {
    namespace = "ir.coffevista.vista_native.core.network"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    api(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.json.jvm)
    testImplementation(libs.kotlinx.coroutines.test)
}
