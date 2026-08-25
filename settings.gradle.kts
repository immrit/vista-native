pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/google") {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "vista_native"
include(":app")
include(":core:common")
include(":core:designsystem")
include(":core:model")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:security")
include(":core:testing")
include(":core:worker")
include(":feature:auth")
include(":feature:shell")
include(":feature:profile")
include(":feature:feed")
include(":feature:search")
include(":feature:chat")
include(":feature:services")
