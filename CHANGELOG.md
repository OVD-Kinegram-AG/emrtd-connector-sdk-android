# Kinegram eMRTD Connector SDK Android - Changelog

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
