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
val vistaVersionCode = injectedValue("vista.versionCode", "VISTA_VERSION_CODE")
    ?.toIntOrNull()
    ?: 4050
val vistaVersionName = injectedValue("vista.versionName", "VISTA_VERSION_NAME") ?: "2.6.3"
val flutterReferenceApplicationId = "ir.coffevista.vista"
val nativeBetaApplicationId = injectedValue("vista.applicationId.beta", "VISTA_BETA_APPLICATION_ID")
    ?: "ir.coffevista.vista_native.beta"
val nativeProductionApplicationId = injectedValue("vista.applicationId.production", "VISTA_PRODUCTION_APPLICATION_ID")
    ?: "ir.coffevista.vista"
check(vistaVersionCode > 4049) {
    "Native versionCode must remain above the published Flutter baseline (4049)."
}

android {
    namespace = "ir.coffevista.vista_native"

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    defaultConfig {
        versionCode = vistaVersionCode
        versionName = vistaVersionName
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
            applicationId = nativeBetaApplicationId
            versionNameSuffix = "-beta"
            buildConfigField("String", "APP_ENVIRONMENT", "\"beta\"")
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${injectedValue("vista.api.beta", "VISTA_BETA_API_BASE_URL") ?: "https://api.coffevista.ir"}\"",
            )
        }
        create("production") {
            dimension = "environment"
            applicationId = nativeProductionApplicationId
            buildConfigField("String", "APP_ENVIRONMENT", "\"production\"")
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

val verifyNativeApplicationIds by tasks.registering {
    group = "verification"
    description = "Validates production in-place upgrade and beta isolation package IDs."
    doLast {
        val productionFlavor = android.productFlavors.firstOrNull { it.name == "production" }
        val betaFlavor = android.productFlavors.firstOrNull { it.name == "beta" }
        check(productionFlavor?.applicationId == nativeProductionApplicationId) {
            "Native production variant must use reconciled application ID: ${productionFlavor?.applicationId}"
        }
        check(betaFlavor?.applicationId?.startsWith("ir.coffevista.vista_native") == true) {
            "Native beta variant must retain isolated beta package ID: ${betaFlavor?.applicationId}"
        }
    }
}

tasks.named("preBuild") {
    dependsOn(verifyNativeApplicationIds)
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:security"))
    implementation(project(":core:worker"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:shell"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:feed"))
    implementation(project(":feature:search"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:services"))

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
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.firebase.messaging)

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
