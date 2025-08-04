# Kinegram eMRTD Connector SDK Android

The Kinegram eMRTD Connector enables your Android app to read and verify
electronic passports / id cards ([eMRTDs][emrtd]).

```
    ┌───────────────┐     Results     ┌─────────────────┐
    │ DocVal Server │────────────────▶│   Your Server   │
    └───────────────┘                 └─────────────────┘
            ▲
            │ WebSocket
            ▼
┏━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                        ┃
┃    eMRTD Connector     ┃
┃                        ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━┛
            ▲
            │ NFC
            ▼
    ┌──────────────┐
    │              │
    │   PASSPORT   │
    │              │
    │   ID CARD    │
    │              │
    │              │
    │   (eMRTD)    │
    │              │
    └──────────────┘
```

The *Kinegram eMRTD Connector* enables the
[Document Validation Server (DocVal)][docval] to communicate with the eMRTD
through a secure WebSocket connection.

## Example App

This project contains a minimal fully functional demo app, that shows the
usage of the SDK.

[Download Example App](distribution.zip)

### System Requirements

* [Android Studio][android]
* Device running Android 7 (API level 24) or later with NFC capabilities

### Running

First enable [adb debugging][debugging] on the mobile device and plug it in.

On a system with a Unix shell and [make][make] run:

```bash
make
```

The (short and very readable) [Makefile](Makefile) covers building, running
and more.

Alternatively just open the project with [Android Studio][android] and click
run.

## Dependencies

The library is hosted in a private Maven repository.  
Make sure to **add the repository** in your `settings.gradle.kts`
(or `settings.gradle`) using the `dependencyResolutionManagement` block, and
then add the dependency to your app module.

### 1. Add the Maven repository

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
	repositories {
		maven("https://git.kurzdigital.com/api/v4/projects/1884/packages/maven")
		// other repositories …
	}
	// …
}
```

<details>
<summary>Groovy</summary>

```groovy
// settings.gradle
dependencyResolutionManagement {
	// …
	repositories {
		maven {
			url 'https://git.kurzdigital.com/api/v4/projects/1884/packages/maven'
		}
		// other repositories …
	}
}
```

</details>

### 2. Add the SDK to your app module

```kts
// app/build.gradle.kts
dependencies {
	implementation("com.kinegram.android:emrtdconnector:<version>")
}
```

You will also need to resolve file-duplication conflicts in the dependencies by
by excluding them:

```kts
android {
	packagingOptions.resources.excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
}
```

<details>
<summary>Groovy</summary>

```groovy
// app/build.gradle
dependencies {
	implementation 'com.kinegram.android:emrtdconnector:<version>'
}

android {
	packagingOptions {
		resources {
			excludes += 'META-INF/versions/9/OSGI-INF/MANIFEST.MF'
		}
	}
}
```

</details>

### Usage and API description

Open the [Dokka Documentation](dokka) in your preferred browser.

There is also a [JavaDoc Documentation](javadoc) available if you prefer the old
JavaDoc style.

## Changelog

[Changelog](CHANGELOG.md)

[emrtd]: https://kta.pages.kurzdigital.com/kta-kinegram-document-validation-service/Security%20Mechanisms
[docval]: https://kta.pages.kurzdigital.com/kta-kinegram-document-validation-service/
[android]: https://developer.android.com/studio
[debugging]: https://developer.android.com/tools/help/adb.html#Enabling
[make]: https://www.gnu.org/software/make/
[add-dependencies]: https://developer.android.com/build/dependencies
[privacy-notice]: https://kinegram.digital/privacy-notice/
