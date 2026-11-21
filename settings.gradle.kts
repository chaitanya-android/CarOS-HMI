pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CarOSMini"

include(":core-api")
include(":core-service")
include(":app-launcher")
include(":app-media")
include(":app-nav")
include(":app-dashboard")
