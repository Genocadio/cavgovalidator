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
//        maven {
//            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
//            credentials {
//                username = "mapbox"
//                password = extra["MAPBOX_DOWNLOADS_TOKEN"] as String
//            }
//        }
    }
}

rootProject.name = "Cavgodrivers"
include(":app")
