plugins {
    id("vista.android.library")
}

android {
    namespace = "ir.coffevista.vista_native.core.network"
}

dependencies {
    implementation(project(":core:common"))
}
