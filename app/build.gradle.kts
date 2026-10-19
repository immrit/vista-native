import groovy.json.JsonSlurper
import java.io.File
import java.util.Properties

plugins {
    id("vista.android.application")
    id("vista.hilt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

hilt {
    enableAggregatingTask = false
}

val keyPropertiesFile = listOf(
    rootProject.file("key.properties"),
    file("key.properties"),
    File("E:/vista/android/key.properties"),
).firstOrNull { it.exists() }

val keyProperties = Properties().apply {
    if (keyPropertiesFile != null && keyPropertiesFile.exists()) {
        keyPropertiesFile.inputStream().use { stream ->
            this.load(stream)
        }
    }
}

fun injectedValue(gradleName: String, environmentName: String, keyPropName: String? = null): String? =
    providers.gradleProperty(gradleName)
        .orElse(providers.environmentVariable(environmentName))
        .orNull
        ?.takeIf { it.isNotBlank() }
        ?: keyPropName?.let { prop -> keyProperties.getProperty(prop)?.takeIf { it.isNotBlank() } }

val rawStoreFile = injectedValue("vista.signing.storeFile", "VISTA_SIGNING_STORE_FILE", "storeFile")
val resolvedStoreFile = rawStoreFile?.let { path ->
    if (File(path).isAbsolute) {
        File(path)
    } else {
        listOf(
            file(path),
            rootProject.file(path),
            file("src/main/$path"),
            File("E:/vista/android/app/$path"),
        ).firstOrNull { it.exists() } ?: file(path)
    }
}

val signingValues = mapOf(
    "storeFile" to resolvedStoreFile?.takeIf { it.exists() }?.absolutePath,
    "storePassword" to injectedValue("vista.signing.storePassword", "VISTA_SIGNING_STORE_PASSWORD", "storePassword"),
    "keyAlias" to injectedValue("vista.signing.keyAlias", "VISTA_SIGNING_KEY_ALIAS", "keyAlias"),
    "keyPassword" to injectedValue("vista.signing.keyPassword", "VISTA_SIGNING_KEY_PASSWORD", "keyPassword"),
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

val generatedProductionFirebaseResources = layout.buildDirectory.dir(
    "generated/res/productionFirebase",
)

val generateProductionFirebaseResources by tasks.registering {
    val firebaseConfig = file("google-services.json")
    inputs.file(firebaseConfig)
    outputs.dir(generatedProductionFirebaseResources)

    doLast {
        fun Map<*, *>.objectValue(name: String): Map<*, *> =
            (this[name] as? Map<*, *>)
                ?: error("google-services.json is missing object: $name")

        fun Map<*, *>.stringValue(name: String): String =
            (this[name] as? String)
                ?.takeIf(String::isNotBlank)
                ?: error("google-services.json is missing value: $name")

        fun xmlEscape(value: String): String = buildString(value.length) {
            value.forEach { character ->
                append(
                    when (character) {
                        '&' -> "&amp;"
                        '<' -> "&lt;"
                        '>' -> "&gt;"
                        '\"' -> "&quot;"
                        '\'' -> "&apos;"
                        else -> character
                    },
                )
            }
        }

        val root = JsonSlurper().parse(firebaseConfig) as? Map<*, *>
            ?: error("google-services.json must contain a JSON object")
        val projectInfo = root.objectValue("project_info")
        val productionClient = (root["client"] as? List<*>)
            ?.filterIsInstance<Map<*, *>>()
            ?.singleOrNull { client ->
                client.objectValue("client_info")
                    .objectValue("android_client_info")
                    .stringValue("package_name") == nativeProductionApplicationId
            }
            ?: error(
                "google-services.json must contain exactly one client for " +
                    nativeProductionApplicationId,
            )
        val clientInfo = productionClient.objectValue("client_info")
        val apiKey = (productionClient["api_key"] as? List<*>)
            ?.filterIsInstance<Map<*, *>>()
            ?.singleOrNull()
            ?.stringValue("current_key")
            ?: error("google-services.json must contain exactly one API key")

        val values = linkedMapOf(
            "google_app_id" to clientInfo.stringValue("mobilesdk_app_id"),
            "gcm_defaultSenderId" to projectInfo.stringValue("project_number"),
            "project_id" to projectInfo.stringValue("project_id"),
            "google_api_key" to apiKey,
            "google_crash_reporting_api_key" to apiKey,
        )
        (projectInfo["firebase_url"] as? String)?.takeIf(String::isNotBlank)?.let {
            values["firebase_database_url"] = it
        }
        (projectInfo["storage_bucket"] as? String)?.takeIf(String::isNotBlank)?.let {
            values["google_storage_bucket"] = it
        }
        (productionClient["oauth_client"] as? List<*>)
            ?.filterIsInstance<Map<*, *>>()
            ?.firstOrNull { it["client_type"] == 3 }
            ?.get("client_id")
            ?.let { it as? String }
            ?.takeIf(String::isNotBlank)
            ?.let { values["default_web_client_id"] = it }

        val valuesDirectory = generatedProductionFirebaseResources.get()
            .dir("values")
            .asFile
        check(valuesDirectory.mkdirs() || valuesDirectory.isDirectory) {
            "Could not create generated Firebase resource directory"
        }
        valuesDirectory.resolve("google_services.xml").writeText(
            buildString {
                appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                appendLine("<resources>")
                values.forEach { (name, value) ->
                    appendLine(
                        "    <string name=\"$name\" translatable=\"false\">" +
                            "${xmlEscape(value)}</string>",
                    )
                }
                appendLine("</resources>")
            },
        )
    }
}

android.sourceSets.getByName("production").res.srcDir(generateProductionFirebaseResources)

// `srcDir` registers the generated location but does not make an ordinary
// task producer an implicit AGP dependency. Ensure the production resource
// merger never packages an empty Firebase resource directory.
tasks.configureEach {
    if (name.contains("ProductionRelease") && name != "generateProductionFirebaseResources") {
        dependsOn(generateProductionFirebaseResources)
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
