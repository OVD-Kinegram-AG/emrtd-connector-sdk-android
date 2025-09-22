import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import com.github.jk1.license.render.*
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import proguard.gradle.ProGuardTask
import org.gradle.api.tasks.bundling.Zip
import java.net.URI

plugins {
	id("com.android.library")
	id("kotlin-android") // Needed for Dokka to document anything
	id("org.jetbrains.dokka") version "1.9.10"
	id("maven-publish")
	id("signing")
	id("com.github.jk1.dependency-license-report") version "2.9"
	id("com.gradleup.shadow") version "9.0.0-rc1"
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

	defaultConfig {
		consumerProguardFiles("consumer-proguard-rules.pro")
		// Expose the library version as a string resource
		buildConfigField("String", "LIBRARY_VERSION", "\"${project.version}\"")
	}

	buildFeatures {
		buildConfig = true
	}
}

configurations {
	create("internalize")
}

val internalizeJar by tasks.registering(ShadowJar::class) {
	archiveClassifier.set("intern")
	configurations = listOf(project.configurations["internalize"])
}


val internalizeTransitive by configurations.creating


val emrtdSdk = "com.kinegram.emrtd:emrtd-sdk-java:2.0.0"

dependencies {
	implementation("org.java-websocket:Java-WebSocket:1.5.5")
	implementation("com.google.android.material:material:1.12.0")
	implementation("androidx.appcompat:appcompat:1.7.0")
	implementation("androidx.activity:activity:1.9.3")
	implementation("androidx.constraintlayout:constraintlayout:2.2.0")
	implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
	implementation("com.google.android.material:material:1.12.0")
	implementation("androidx.core:core:1.15.0")

	// We "internalize" (i.e. include in the final artifact) our internal java
	// sdk, because users of this Android library do not have access to the
	// java-sdk.
	"internalize"(emrtdSdk) { isTransitive = false }
	implementation(tasks.named("internalizeJar").get().outputs.files)

	// However, they still need the transitive dependencies that come from the
	// java sdk, so we have to extract them and declare them directly.
	"internalizeTransitive"(emrtdSdk)
}

// Resolve after evaluation, otherwise Android Studio has problems building its index
afterEvaluate {
	// See comment in dependencies block
	val transitiveDependencies = internalizeTransitive
		.resolvedConfiguration
		.firstLevelModuleDependencies
		.first()
		.children

	dependencies {
		transitiveDependencies.forEach { dep ->
			implementation("${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}")
		}
	}
}

buildscript {
	dependencies {
		classpath("org.jetbrains.dokka:dokka-base:1.9.10")
		classpath("com.guardsquare:proguard-gradle:7.7.0")
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
				url.set(
					URI("https://javadoc.io/doc/org.java-websocket/Java-WebSocket/1.5.3/")
						.toURL()
				)
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
			url =
				uri("https://git.kurzdigital.com/api/v4/projects/${System.getenv("CI_PROJECT_ID")}/packages/maven")
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

// Because the emrtd-sdk-java is an internal dependency that we do not want to publish but have to
// ship it with this library, we obfuscate it to protect our IP. This happens as the very last step
// by unzipping the final .aar file and using ProGuard to obfuscate the .jar files in it.
afterEvaluate {
	val variant = "release"
	val variantCap = variant.replaceFirstChar(Char::uppercase)

	// Cleanup from previous runs
	project.delete(layout.buildDirectory.dir("aarObfTmp"))

	val aarZipTask = tasks.withType<Zip>()
		.firstOrNull {
			it.archiveFileName.get().endsWith(".aar") &&
				it.name.contains(variant, ignoreCase = true)
		}
		?: error("No AAR‑producing task found for variant '$variant'")
	val unpackDir = layout.buildDirectory.dir("aarUnpacked")
	val obfDir = layout.buildDirectory.dir("aarObfTmp")

	val obfuscateAar = tasks.register("obfuscate${variantCap}Aar", ProGuardTask::class) {
		dependsOn(aarZipTask)

		doFirst {
			delete(unpackDir)
			copy {
				from(zipTree(aarZipTask.archiveFile))
				into(unpackDir)
			}
		}

		// In the final .aar file, there are two jar files: A "classes.jar" in the root of the .aar
		// and the emrtd-sdk-java in the libs directory. We unpack the .aar and then obfuscate the
		// lib.
		injars(fileTree(unpackDir) { include("classes.jar", "libs/*.jar") })
		outjars(obfDir)
		dontwarn()

		configuration("proguard-rules.pro")

		libraryjars(android.bootClasspath)
		libraryjars(configurations.getByName("releaseRuntimeClasspath").files)

		doLast {
			// Delete originals
			fileTree(unpackDir).matching {
				include("classes.jar", "libs/*.jar")
			}.forEach { it.delete() }

			// Place classes.jar back into root of the AAR
			copy {
				from(obfDir) { include("classes.jar") }
				into(unpackDir)
			}

			// Place every other obfuscated jar in libs/
			copy {
				from(obfDir) { exclude("classes.jar") }
				into(File(unpackDir.get().asFile, "libs"))
			}

			// Re‑zip over original AAR
			ant.invokeMethod(
				"zip", mapOf(
					"destfile" to aarZipTask.archiveFile.get().asFile.absolutePath,
					"basedir" to unpackDir.get().asFile.absolutePath,
					"update" to "false"
				)
			)
		}
	}

	// Make sure that the obfuscated jar is used for publishing
	publishing {
		publications.named<MavenPublication>("release") {
			// Drop the non‑obfuscated one
			artifacts.removeIf { it.extension == "aar" }

			// Attach the obfuscated AAR
			artifact(aarZipTask.archiveFile) {
				builtBy(obfuscateAar)
				extension = "aar"
			}
		}
	}

	tasks.named("assemble${variantCap}").configure {
		finalizedBy(obfuscateAar)
	}
}
