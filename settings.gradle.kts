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
		maven {
			url = uri("https://git.kurzdigital.com/api/v4/projects/1739/packages/maven")
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
 