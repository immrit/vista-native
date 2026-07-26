plugins {
    `kotlin-dsl`
}

group = "ir.coffevista.buildlogic"

val buildLogicOutput = providers.gradleProperty("vista.buildLogicOutput")
    .orElse("build-logic-output")
layout.buildDirectory.set(
    rootProject.layout.projectDirectory.dir("../.gradle/${buildLogicOutput.get()}"),
)

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.hilt.gradle.plugin)
    implementation(libs.javapoet)
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
        register("vistaHilt") {
            id = "vista.hilt"
            implementationClass = "VistaHiltPlugin"
        }
    }
}
