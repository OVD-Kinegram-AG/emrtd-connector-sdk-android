# Kinegram eMRTD Connector SDK Android - Changelog

## 2.1.22
* Update eMRTD library

## 2.1.21
* Update eMRTD library

## 2.1.20
* Update eMRTD library

## 2.1.19
* Improve detection of NFC errors

## 2.1.18
* Fix returning exception

## 2.1.17
* Add a toJSON() method to EmrtdPassport

## 2.1.16
* Streamline EmrtdConnectorActivity API

## 2.1.15
* Streamline EmrtdConnectorActivity API

## 2.1.14
* Improve error handling

## 2.1.13
* Migrate to NfcAdapter.enableReaderMode()

## 2.1.12
* Disable foreground dispatch in onPause()

## 2.1.11
* Improve design of EmrtdConnectorActivity

## 2.1.10
* Improve messages in EmrtdConnectorActivity

## 2.1.9
* Migrate Dokka from V1 to V2

## 2.1.8
* Add a check box to use EmrtdConnectorActivity in the example app

## 2.1.7
* Make names of intent extras public

## 2.1.6
* Continue access control on failed master file selection

## 2.1.5
* Fix bug in Passive Authentication for some passports (e.g. French ID)

## 2.1.4
* Fix bug when reading passports that do not support PACE

## 2.1.3
* Fix PACE/BAC order

## 2.1.2
* Fix missing APDU response data in traces

## 2.1.1
* Also include raw IsoDep data in OpenTelemetry traces during diagnostic sessions

## 2.1.0
* Requires a DocVal server with version `1.9.0` or newer
* Add support for monitoring messages during diagnostic sessions
* Add support for OpenTelemetry using `EmrtdConnector#setTracerProvider(...)`

## 2.0.0
* Migrate to the new DocVal v2 WebSocket API, which significantly improves reading speed on high-latency internet connections

### Breaking Changes
* You must add `android.packagingOptions.resources.excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"` to your app's `build.gradle.kts` file
* The minimum Android API level is now 24 (was 21)
* Requires a DocVal server with version `1.8.0` or newer
* The Android eMRTD Connector SDK is no longer published to Maven Central (see README for new repository address)

## 1.2.0
* Add new `ConnectionOptions.Builder#setEnableDiagnostics(boolean)` method to enable diagnostics in the DocVal server.

## 1.1.0
* Add new overload for `EmrtdConnector#connect(...)` method that accepts a `ConnectionOptions` parameter
* Deprecate old `EmrtdConnector#connect(...)` method overloads that do not use `ConnectionOptions`
* Add option to send custom HTTP headers in handshake using `ConnectionOptions.Builder#setHttpHeaders(...)`

## 1.0.1
* Publish to Maven Central. Remove the old `.arr` file from your app's `libs`
  folder and update the Gradle dependency as described in the README.

## 1.0.0
* Improve documentation and example app
* Change library package ~~com.kurzdigital.android.emrtdconnector~~ to `com.kinegram.android.emrtdconnector`!
  Update your import statements accordingly.

## 0.0.14
* Improve Usage and API documentation
* Generate Dokka documentation
* Include library module as aar in distribution project
* Improve Example app code

## 0.0.13
* Cleanup project

## 0.0.12
* Fix WebSocket Connection for Android 23 and older
* Fix Timeout description comments in `ClosedListener`.
* Update Build Tools and Dependencies

## 0.0.11
* Also parse the optional field "files_binary" in the EmrtdPassport Result JSON

## 0.0.10
* Additions to the documentation

## 0.0.9
* Show if passport is expired

## 0.0.8
* Show Details if Passive Authentication failed
* Enforce "All Caps" for Document Number
* Improve Documentation

## 0.0.7
* Use more suitable ApplicationId for Example App
* Make PendingIntent Mutable (for NFC-Tag Discovered Intent)

## 0.0.6
* Change App name to "eMRTD Connector"

## 0.0.5
* Improve presentation of the Result
* Improve Documentation

## 0.0.4
* Create Distribution Project for customers
* Add documentation and comments
* Add Java Sample Code
* Improve Layout and UI of Sample App

## 0.0.3
* Improve Handling of session closes

## 0.0.2
* Add Build Type QSU

## 0.0.1
* Initial version
