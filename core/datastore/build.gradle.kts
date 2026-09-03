plugins {
    // Protobuf 0.9.4 must observe AGP before its own callback runs; applying
    // these explicitly preserves the shared convention below while avoiding
    // its Java-source-set fallback during a full multi-module configuration.
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("vista.android.library")
    alias(libs.plugins.protobuf)
}

android {
    namespace = "ir.coffevista.vista_native.core.datastore"
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.protobuf.javalite)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
