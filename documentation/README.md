# Kinegram eMRTD Connector SDK Android

The Kinegram eMRTD Connector enables your Android app to read and verify electronic passports / id
cards ([eMRTDs][emrtd]).

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

The *Kinegram eMRTD Connector* enables the [Document Validation Server (DocVal)][docval] to
communicate with the eMRTD through a secure WebSocket connection.

## Example App

This project contains an Example App to demonstrate usage and functionality.

### Requirements

* [Android Studio][android]
* Device running Android 5 (API level 21) or later with NFC capabilities
* [Access to our Maven registry][registry-credentials]

### Running

[Enable adb debugging][debugging] on your device and connect it with USB.

Open the project with [Android Studio][android] and click run.

## Include the Kinegram eMRTD Connector in your app

[Add the dependencies][add-dependencies] to your app's gradle build configuration.

1. Configure your app-level build.gradle (`app/build.gradle`) file to include the `emrtdconnector`
   dependency.
2. Replace `<version>` with the version you want to use. You can find the latest version in the
   [releases][emrtd-connector-releases].

<details open>
<summary>Kotlin (app/build.gradle.kts)</summary>

```kotlin
dependencies {
	...
	implementation("com.kinegram.android:emrtdconnector:<version>")
}
```

</details>

<details>

<summary>Groovy (app/build.gradle)</summary>

```groovy
dependencies {
	...
	implementation 'com.kinegram.android:emrtdconnector:<version>'
}
```

</details>

### Usage and API description

Open the [Dokka Documentation](dokka) in your preferred browser.

There is also a [JavaDoc Documentation](javadoc) available if you prefer the old
JavaDoc style.

## Changelog

[Changelog](CHANGELOG.md)

## Privacy Notice

ℹ️ [Privacy Notice][privacy-notice]

[emrtd]: https://kta.pages.kurzdigital.com/kta-kinegram-document-validation-service/Security%20Mechanisms
[docval]: https://kta.pages.kurzdigital.com/kta-kinegram-document-validation-service/
[android]: https://developer.android.com/studio
[registry-credentials]: https://kurzdigital.atlassian.net/wiki/spaces/KDS/pages/17829696/12.+KDS+Maven+Gradle+Setup+Amazon+S3+and+Gitlab+Registry
[debugging]: https://developer.android.com/tools/help/adb.html#Enabling
[emrtd-connector-releases]: https://github.com/OVD-Kinegram-AG/emrtd-connector-sdk-android/releases
[add-dependencies]: https://developer.android.com/build/dependencies
[privacy-notice]: https://kinegram.digital/privacy-notice/
