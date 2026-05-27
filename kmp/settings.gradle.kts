pluginManagement {
    repositories {
        // Optional: Uncomment the following Aliyun mirrors if you experience connection/403 issues with default repos:
        // maven("https://maven.aliyun.com/repository/public")
        // maven("https://maven.aliyun.com/repository/google")
        // maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Optional: Uncomment the following Aliyun mirrors if you experience connection/403 issues with default repos:
        // maven("https://maven.aliyun.com/repository/public")
        // maven("https://maven.aliyun.com/repository/google")
        google()
        mavenCentral()
    }
}

rootProject.name = "meow-circle-kmp"

include(":shared")
include(":androidApp")
