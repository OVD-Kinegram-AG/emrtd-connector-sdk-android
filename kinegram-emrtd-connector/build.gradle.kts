import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import java.net.URL

plugins {
	id("com.android.library")
	id("kotlin-android") // Needed for Dokka to document anything
	id("org.jetbrains.dokka") version "1.9.10"
	id("maven-publish")
	id("signing")
}

android {
	namespace = "com.kinegram.android.emrtdconnector"
	compileSdk = 34

	defaultConfig {
		minSdk = 16
	}

	buildTypes {
		debug {}
		release {
			isMinifyEnabled = false
		}
	}
}

dependencies {
	api("org.java-websocket:Java-WebSocket:1.5.5")
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
			externalDocumentationLink {
				// Somehow there is an error (AccessDenied) if version is `1.5.5` or `latest`
				// With version `1.5.3` everything works fine
				url.set(URL("https://javadoc.io/doc/org.java-websocket/Java-WebSocket/1.5.3/"))
			}
		}
	}
}
