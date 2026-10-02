pluginManagement {
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
        mavenCentral()
    }
}

rootProject.name = "PixiJoy"
include(":app")
include(":designsystem")
include(":core:common")
include(":core:data")
include(":core:media")
include(":photo")
include(":movie")
include(":ml")
include(":billing")
include(":benchmark")
include(":feature_titles3d")
include(":feature_cg")
include(":feature_ar")
include(":feature_ffmpeg")
