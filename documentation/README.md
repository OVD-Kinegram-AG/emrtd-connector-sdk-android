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
* Device running Android 7 (API level 24) or later with NFC capabilities
* Access to our private Maven repository

### Running

[Enable adb debugging][debugging] on your device and connect it with USB.

Open the project with [Android Studio][android] and click run.

## Include the Kinegram eMRTD Connector in your app

[Add the dependencies][add-dependencies] to your app's gradle build configuration.

1. Configure your settings.gradle file to use our private Maven repository
2. Configure your app-level build.gradle (`app/build.gradle`) file to include the `emrtdconnector`
   dependency.
3. Replace `<version>` with the version you want to use.

<details open>
<summary>Kotlin</summary>

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
	repositories {
		...
		maven {
			url = uri("https://git.kurzdigital.com/api/v4/groups/326/-/packages/maven")
			name = "kd-gitlab"
			credentials(HttpHeaderCredentials::class) {
				name = "your username"
				value = "your token"
			}
			authentication {
				create("header", HttpHeaderAuthentication::class)
			}
		}
	}
}
```
```kotlin
// app/build.gradle.kts
dependencies {
	...
	implementation("com.kinegram.android:emrtdconnector:<version>")
}
```


</details>


<details>

<summary>Groovy</summary>

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        ...
        maven {
            url "https://git.kurzdigital.com/api/v4/groups/326/-/packages/maven"
            name "kd-gitlab"
            credentials(PasswordCredentials) {
                username = 'your username'
                password = 'your token'
            }
            authentication {
                basic(BasicAuthentication)
            }
        }
    }
}
```

```groovy
// app/build.gradle
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
[debugging]: https://developer.android.com/tools/help/adb.html#Enabling
[add-dependencies]: https://developer.android.com/build/dependencies
[privacy-notice]: https://kinegram.digital/privacy-notice/
