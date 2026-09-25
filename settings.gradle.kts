rootProject.name = "MyCV-Android"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    // One version catalog for both repos: it lives in the CvApi submodule.
    versionCatalogs {
        create("libs") {
            from(files("CvApi/gradle/libs.versions.toml"))
        }
    }
}

include(":androidApp")
include(":shared")
project(":shared").projectDir = file("CvApi/shared")
