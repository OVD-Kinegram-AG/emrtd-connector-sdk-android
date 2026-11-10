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
        // Needed for emrtd-sdk-java
        maven {
            name = "kinegram"
            url = uri("https://maven.registry.kinegram.digital/releases")
            content { includeGroupByRegex("com\\.kinegram\\..*") }
        }
        // Local builds of emrtd-sdk-java for tests before a release
        mavenLocal {
            content { includeGroupByRegex("com\\.kinegram\\..*") }
        }
    }
}

rootProject.name = "Kinegram eMRTD Connector SDK Android"
include(":kinegram-emrtd-connector", ":app")
