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

## Versioning

This library follows [Semantic Versioning 2.0.0][semver]

## Example App

This project contains an Example App to demonstrate usage and functionality.

### System Requirements

* [Android Studio][android]
* Device running Android 7 (API level 24) or later with NFC capabilities

### Running

First enable [adb debugging][debugging] on the mobile device and plug it in.

On a system with a Unix shell and [make][make] run:

```bash
make
```

The (short and very readable) [Makefile](Makefile) covers building, running and more.

Alternatively just open the project with [Android Studio][android] and click
run.

## Include the Kinegram eMRTD Connector in your app

[Add the dependencies][add-dependencies] to your app's gradle build configuration.

1. Configure your app-level build.gradle (`app/build.gradle`) file to include the `emrtdconnector`
   dependency.
2. Replace `<version>` with the version you want to use.
3. Resolve file-duplication conflicts like shown below

<details open>
<summary>Kotlin</summary>

```kotlin
// app/build.gradle.kts
dependencies {
	...
	implementation("com.kinegram.android:emrtdconnector:<version>")
}
```
```kotlin
// build.gradle.kts
android {
	...
    // Resolve all conflicts of duplicated files in dependencies
	packagingOptions.resources.excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
}
```

</details>

<details>

<summary>Groovy</summary>

```groovy

// app/build.gradle
dependencies {
	...
	implementation 'com.kinegram.android:emrtdconnector:<version>'
}
```
```groovy
// build.gradle
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

[emrtd]: https://kta.pages.kurzdigital.com/kta-kinegram-document-validation-service/SecurityMechanisms
[docval]: https://kta.pages.kurzdigital.com/kta-kinegram-document-validation-service/
[android]: https://developer.android.com/studio
[debugging]: https://developer.android.com/tools/help/adb.html#Enabling
[make]: https://www.gnu.org/software/make/
[add-dependencies]: https://developer.android.com/build/dependencies
[privacy-notice]: https://kinegram.digital/privacy-notice/
[semver]: https://semver.org/spec/v2.0.0.html
