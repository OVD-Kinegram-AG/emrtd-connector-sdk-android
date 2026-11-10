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
        mavenLocal()
        google()
        mavenCentral()
        // Needed for emrtd-sdk-java
        maven {
            name = "kinegram"
            url = uri("https://maven.registry.kinegram.digital/releases")
            content { includeGroupByRegex("com\\.kinegram\\..*") }
        }
    }
}

rootProject.name = "Kinegram eMRTD Connector SDK Android"
include(":kinegram-emrtd-connector", ":app")
