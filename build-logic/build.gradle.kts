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
        register("vistaAndroidLibrary") {
            id = "vista.android.library"
            implementationClass = "VistaAndroidLibraryPlugin"
        }
        register("vistaKotlinLibrary") {
            id = "vista.kotlin.library"
            implementationClass = "VistaKotlinLibraryPlugin"
        }
    }
}
