plugins {
    id("vista.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ir.coffevista.vista_native.core.designsystem"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.kotlinx.coroutines.android)

    constraints {
        implementation("androidx.exifinterface:exifinterface:1.3.7")
    }

    testImplementation(libs.junit)
}
