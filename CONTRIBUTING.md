# Developer Guidelines

This guide is intended for developers of this library. For information on how to
use the library, please consult the [README](./README.md).

## Changelog

All relevant changes for the users of the library should be documented in the
[CHANGELOG.md](./CHANGELOG.md) file. When you edit a changelog entry for an
already released version, please also update the related GitHub release.

## Publishing a new version

To publish a new version

- Update the version number in [`gradle.properties`](gradle.properties)
- Commit and push a new git tag

Please follow the [Semantic Versioning 2.0.0][semconv] convention when choosing
the next version number.

[semconv]: https://semver.org/spec/v2.0.0.html
