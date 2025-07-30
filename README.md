# Kinegram eMRTD Connector SDK Android

Also see [distribution/README.md](distribution/README.md) for information on how
to use the library.

## Prerequisites

This project uses our internal [emrtd-sdk-java]. To be able to download it, you
must be logged in to our Gitlab Maven Registry. Details are described in our
[wiki][wiki-maven-registry].

## Folder Structure

- `kinegram-emrtd-connector`: The library itself
- `app`: Test app for the library for development
- `distribution`: Documentation and example app of the library, intended for the
  end-user

## Architecture and relation to other projects

The eMRTD Connector requires our closed-source `emrtd-sdk-java` library. To avoid
exposing that private SDK, the build embeds ("shades") its JAR directly into the
AAR that Gradle produces for the Android connector. All open-source libraries
that the Java SDK needs are listed as normal `implementation` dependencies, so
they are fetched from public Maven repositories while the private SDK itself
never leaves the AAR.

Just before publication the AAR is unpacked, both `classes.jar` and the shaded
Java SDK are run through ProGuard, and the obfuscated result is zipped back up.
The connector that finally gets uploaded therefore contains the private code,
but only in an obfuscated form.

For application developers this means that adding a single dependency,

```
implementation("com.kinegram.android:emrtdconnector:<version>")
```

is enough; no credentials for our private registry are required, and the
proprietary Java SDK remains invisible and protected inside the published AAR.

## Publishing a new version

To publish a new version

- Update the version number in [`gradle.properties`](gradle.properties) and
  [`distribution/gradle/libs.versions.toml`](distribution/gradle/libs.versions.toml)
- Commit and push a new git tag

[emrtd-sdk-java]: https://git.kurzdigital.com/kta/kinegram-emrtd-java-sdk/
[wiki-maven-registry]: https://kurzdigital.atlassian.net/wiki/spaces/KDS/pages/17829696/12.+KDS+Maven+Gradle+Setup+Amazon+S3+and+Gitlab+Registry
