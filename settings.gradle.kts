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
            // Material 3 Expressive (MaterialExpressiveTheme, emphasized type, shape morphing, LoadingIndicator)
            // is public from material3 1.5; the Compose BOM still pins 1.4. Switch back to the BOM's
            // androidx-compose-material3 once a BOM ships material3 1.5 stable.
            library("material3-expressive", "androidx.compose.material3", "material3").version("1.5.0-beta02")
        }
    }
}

include(":androidApp")
include(":shared")
project(":shared").projectDir = file("CvApi/shared")
