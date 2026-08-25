plugins {
    id("vista.kotlin.library")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
}
