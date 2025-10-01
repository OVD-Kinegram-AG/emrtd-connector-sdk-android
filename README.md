# Kinegram eMRTD Connector SDK Android

Also see [distribution/README.md](distribution/README.md) for information on how
to use the library.

## Folder Structure

- `kinegram-emrtd-connector`: The library itself
- `app`: Test app for the library for development
- `distribution`: Documentation and example app of the library, intended for the
  end-user

## Publishing a new version

To publish a new version

- Update the version number in [`gradle.properties`](gradle.properties) and
  [`distribution/gradle/libs.versions.toml`](distribution/gradle/libs.versions.toml)
- Commit and push a new git tag

[emrtd-sdk-java]: https://git.kurzdigital.com/kta/kinegram-emrtd-java-sdk/
[wiki-maven-registry]: https://kurzdigital.atlassian.net/wiki/spaces/KDS/pages/17829696/12.+KDS+Maven+Gradle+Setup+Amazon+S3+and+Gitlab+Registry
