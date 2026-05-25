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
        maven { url = uri("https://jitpack.io") }
    }
    // Reuse the main project's version catalog so mod builds stay in sync.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

// mod-api lives in the parent project — reference it by relative path.
include(":mod-api")
project(":mod-api").projectDir = File("../mod-api")

rootProject.name = "KanataMods"

// Each folder here is one mod. Add new mods below.
include(":source-example")
