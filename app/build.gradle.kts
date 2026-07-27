plugins {
    id("vista.android.application")
    id("vista.hilt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

hilt {
    enableAggregatingTask = false
}

fun injectedValue(gradleName: String, environmentName: String): String? =
    providers.gradleProperty(gradleName)
        .orElse(providers.environmentVariable(environmentName))
        .orNull
        ?.takeIf { it.isNotBlank() }

val signingValues = mapOf(
    "storeFile" to injectedValue("vista.signing.storeFile", "VISTA_SIGNING_STORE_FILE"),
    "storePassword" to injectedValue("vista.signing.storePassword", "VISTA_SIGNING_STORE_PASSWORD"),
    "keyAlias" to injectedValue("vista.signing.keyAlias", "VISTA_SIGNING_KEY_ALIAS"),
    "keyPassword" to injectedValue("vista.signing.keyPassword", "VISTA_SIGNING_KEY_PASSWORD"),
)
val configuredSigningValues = signingValues.values.count { it != null }
check(configuredSigningValues == 0 || configuredSigningValues == signingValues.size) {
    "Release signing is partially configured. Inject all four VISTA_SIGNING_* values or none."
}

android {
    namespace = "ir.coffevista.vista_native"

    defaultConfig {
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (configuredSigningValues == signingValues.size) {
            create("injectedRelease") {
                storeFile = file(signingValues.getValue("storeFile")!!)
                storePassword = signingValues.getValue("storePassword")
                keyAlias = signingValues.getValue("keyAlias")
                keyPassword = signingValues.getValue("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("beta") {
            dimension = "environment"
            applicationId = "ir.coffevista.vista_native"
            versionNameSuffix = "-beta"
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${injectedValue("vista.api.beta", "VISTA_BETA_API_BASE_URL") ?: "https://api.coffevista.ir"}\"",
            )
        }
        create("production") {
            dimension = "environment"
            applicationId = "ir.coffevista.vista"
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${injectedValue("vista.api.production", "VISTA_PRODUCTION_API_BASE_URL") ?: "https://api.coffevista.ir"}\"",
            )
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("injectedRelease")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:security"))
    implementation(project(":core:worker"))
    implementation(project(":feature:auth"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime)
    implementation(libs.hilt.android)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
