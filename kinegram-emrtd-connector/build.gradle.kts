import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL
import com.github.jk1.license.render.*

plugins {
	id("com.android.library")
	id("kotlin-android") // Needed for Dokka to document anything
	id("org.jetbrains.dokka") version "1.9.10"
	id("maven-publish")
	id("signing")
	id("com.github.jk1.dependency-license-report") version "2.9"
}

android {
	namespace = "com.kinegram.android.emrtdconnector"
	compileSdk = 35

	defaultConfig {
		minSdk = 24
	}

	buildTypes {
		debug {}
		release {
			isMinifyEnabled = false
		}
	}

	publishing {
		singleVariant("release") {
			withSourcesJar()
		}
	}
}

dependencies {
	implementation("org.java-websocket:Java-WebSocket:1.5.5")
	implementation("com.google.android.material:material:1.12.0")
	implementation("androidx.appcompat:appcompat:1.7.0")
	implementation("androidx.activity:activity:1.9.3")
	implementation("androidx.constraintlayout:constraintlayout:2.2.0")
	implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
	implementation("com.google.android.material:material:1.12.0")
	implementation("androidx.core:core:1.15.0")

	implementation("com.kinegram.emrtd:emrtd-sdk-java:2.0.0")
}

buildscript {
	dependencies {
		classpath("org.jetbrains.dokka:dokka-base:1.9.10")
	}
}

tasks.withType<DokkaTask>().configureEach {
	pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
		customAssets = listOf(file("logo-icon.svg"))
		footerMessage = "© 2024 OVD Kinegram AG"
	}
	moduleName.set("Kinegram eMRTD Connector")
	dokkaSourceSets {
		configureEach {
			displayName.set("Android")
			skipDeprecated.set(true)
			suppressInheritedMembers.set(true)
			includes.from("Module.md")
			perPackageOption {
				matchingRegex.set(".*\\.internal.*")
				suppress.set(true)
			}
			externalDocumentationLink {
				// Somehow there is an error (AccessDenied) if version is `1.5.5` or `latest`
				// With version `1.5.3` everything works fine
				url.set(URL("https://javadoc.io/doc/org.java-websocket/Java-WebSocket/1.5.3/"))
			}
		}
	}
}

publishing {
	publications {
		register<MavenPublication>("release") {
			groupId = "com.kinegram.android"
			artifactId = "emrtdconnector"

			afterEvaluate {
				from(components["release"])
			}

			pom {
				name.set("Kinegram eMRTD Connector SDK Android")
				description.set("The Kinegram eMRTD Connector enables your Android app to read and verify electronic passports / id cards.")
				url.set("https://kinegram.digital/mobile-chip-sdk/emrtd-connector/")
				developers {
					developer {
						id.set("OVD Kinegram AG")
						name.set("OVD Kinegram AG")
						email.set("contact@kinegram.digital")
						url.set("https://kinegram.digital/")
						timezone.set("Europe/Zurich")
					}
				}
			}
		}
	}
	repositories {
		maven {
			url = uri("https://git.kurzdigital.com/api/v4/projects/${System.getenv("CI_PROJECT_ID")}/packages/maven")
			credentials(HttpHeaderCredentials::class) {
				name = "Job-Token"
				value = System.getenv("CI_JOB_TOKEN")
			}
			authentication {
				create("header", HttpHeaderAuthentication::class)
			}
		}
	}
}

licenseReport {
	configurations = arrayOf("releaseRuntimeClasspath")
	renderers = arrayOf(TextReportRenderer())
}
