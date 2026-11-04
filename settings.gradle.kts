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
        maven {
            url = uri("https://git.kurzdigital.com/api/v4/groups/326/-/packages/maven")
            name = "gitlab-maven"
            credentials(HttpHeaderCredentials::class) {
                name = providers.gradleProperty("gitLabUserName").orNull
                value = providers.gradleProperty("gitLabPrivateToken").orNull
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        }
    }
}

rootProject.name = "Kinegram eMRTD Connector SDK Android"
include(":kinegram-emrtd-connector", ":app")
