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
		mavenLocal()
		maven("https://git.kurzdigital.com/api/v4/projects/1884/packages/maven")
		google()
		mavenCentral()
	}
}

rootProject.name = "Kinegram eMRTD Connector SDK Android Sample"
include(":app")
