# Developer Guidelines

This guide is intended for developers of this library. For information on how to
use the library, please consult the [README](./README.md).

## Changelog

All relevant changes for the users of the library should be documented in the
[CHANGELOG.md](./CHANGELOG.md) file. When you edit a changelog entry for an
already released version, please also update the related GitHub release.

## Publishing a new version

When you push a tag in the format `x.y.z`, a GitHub action will run that

- Publishes the release to Maven Central.
- Creates a GitHub release based on the content of the 
  [CHANGELOG.md](./CHANGELOG.md) file.
- Publishes the latest version of the documentation (using GitHub Pages).

Please follow the [Semantic Versioning 2.0.0][semconv] convention when choosing
the next version number.

## Third Party Notices

Whenever you make changes to one of the dependency, run the following Makefile
target to also update the [THIRD-PARTY-NOTICES.txt](./THIRD-PARTY-NOTICES.txt)
file.

```bash
make update-license-report
```

[semconv]: https://semver.org/spec/v2.0.0.html