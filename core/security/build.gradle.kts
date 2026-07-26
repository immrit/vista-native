plugins {
    id("vista.android.library")
}

android {
    namespace = "ir.coffevista.vista_native.core.security"
}

dependencies {
    implementation(project(":core:model"))
    testImplementation(libs.junit)
}
