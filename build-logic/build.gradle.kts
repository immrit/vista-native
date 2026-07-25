plugins {
    `kotlin-dsl`
}

group = "ir.coffevista.buildlogic"

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("vistaAndroidApplication") {
            id = "vista.android.application"
            implementationClass = "VistaAndroidApplicationPlugin"
        }
    }
}
