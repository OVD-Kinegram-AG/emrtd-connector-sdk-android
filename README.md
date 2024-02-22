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

### Running

[Enable adb debugging][debugging] on your device and connect it with USB.

Open the project with [Android Studio][android] and click run.

## Include the Kinegram eMRTD Connector in your app

[Add the dependencies][add-dependencies] to your app's gradle build configuration.

1. Copy the latest `kinegram-emrtd-connector-<version>.aar` from [Releases][emrtd-connector-releases]
   to your app's `libs` directory (`app/libs`).
2. Configure your app-level build.gradle (`app/build.gradle`) file to include local dependencies and
   the [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket) dependency.

<details open>
<summary>Kotlin (app/build.gradle.kts)</summary>

```kotlin
dependencies {
	...
	implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
	implementation("org.java-websocket:Java-WebSocket:1.5.5")
}
```

</details>

<details>

<summary>Groovy (app/build.gradle)</summary>

```groovy
dependencies {
	...
	implementation fileTree(dir: 'libs', include: ['*.jar', '*.aar'])
	implementation 'org.java-websocket:Java-WebSocket:1.5.5'
}
```

</details>

### Usage and API description

Open the [Dokka Documentation][documentation-dokka] in your preferred browser.

There is also a [JavaDoc Documentation][documentation-javadoc] available if you prefer the old JavaDoc
style.

## Changelog

[Changelog](CHANGELOG.md)

## Privacy Notice

ℹ️ [Privacy Notice](eMRTD%20Connector%20App%20DocVal%20Server_Privacy%20Notice.pdf)

[emrtd]: https://kta.pages.kurzdigital.com/kta-kinegram-document-validation-service/Security%20Mechanisms
[docval]: https://kta.pages.kurzdigital.com/kta-kinegram-document-validation-service/
[android]: https://developer.android.com/studio
[debugging]: https://developer.android.com/tools/help/adb.html#Enabling
[emrtd-connector-releases]: https://github.com/OVD-Kinegram-AG/emrtd-connector-sdk-android/releases
[add-dependencies]: https://developer.android.com/build/dependencies
[documentation-dokka]: https://ovd-kinegram-ag.github.io/emrtd-connector-sdk-android/dokka
[documentation-javadoc]: https://ovd-kinegram-ag.github.io/emrtd-connector-sdk-android/javadoc
