.PHONY: update-license-report

PACKAGE = com.kinegram.android.emrtdconnectorapp
VERSION = $(shell sed -n 's/^version=//p' gradle.properties | tail -1)
REFERENCE_PRODUCT = emrtd-connector-android
REFERENCE_DEST = s3://$(DOCS_REFERENCE_BUCKET)/reference/$(REFERENCE_PRODUCT)/$(VERSION)
IMMUTABLE_CACHE = --cache-control "public, max-age=31536000, immutable"

all: debug install start

debug:
	./gradlew assembleDebug

lint:
	./gradlew :kinegram-emrtd-connector:lintDebug

aar: lint
	./gradlew :kinegram-emrtd-connector:assembleRelease

install:
	adb $(TARGET) install -r \
		app/build/outputs/apk/debug/app-debug.apk

start:
	adb $(TARGET) shell 'am start -n \
		$(PACKAGE)/$(PACKAGE).MainActivity'

uninstall:
	adb $(TARGET) uninstall $(PACKAGE)

update-license-report:
	./gradlew :kinegram-emrtd-connector:generateLicenseReport
	grep -Ev '^(This report was generated at|[[:space:]]*Dependency License Report for )' \
		kinegram-emrtd-connector/build/reports/dependency-license/THIRD-PARTY-NOTICES.txt \
	| sed -e '/./,$$!d' \
		-e :a -e '/^\n*$$/{$$d;N;ba' -e '}' \
	> ./THIRD-PARTY-NOTICES.txt

dokka:
	./gradlew :kinegram-emrtd-connector:dokkaGenerateHtml :kinegram-emrtd-connector:dokkaGenerateJavadoc
	mkdir -p _site
	rm -rf _site/dokka _site/javadoc
	cp -r kinegram-emrtd-connector/build/dokka/html _site/dokka
	cp -r kinegram-emrtd-connector/build/dokka/javadoc _site/javadoc

pages:
	tools/pages

# Upload the API references to docs.kinegram.digital/reference/ and point
# "latest" at the new version. Requires AWS credentials and the DOCS_REFERENCE_*
# variables in the environment.
publish_reference: dokka
	@if [ "$(GITHUB_REF_TYPE)" = "tag" ] && [ "$(GITHUB_REF_NAME)" != "$(VERSION)" ]; then \
		echo "Tag '$(GITHUB_REF_NAME)' does not match version '$(VERSION)' from gradle.properties"; \
		exit 1; \
	fi
	aws s3 cp _site/dokka "$(REFERENCE_DEST)/dokka" --recursive $(IMMUTABLE_CACHE)
	aws s3 cp _site/javadoc "$(REFERENCE_DEST)/javadoc" --recursive $(IMMUTABLE_CACHE)
	ETAG=$$(aws cloudfront-keyvaluestore describe-key-value-store \
		--kvs-arn "$(DOCS_REFERENCE_KVS_ARN)" --region us-east-1 \
		--query ETag --output text) && \
	aws cloudfront-keyvaluestore put-key --key "$(REFERENCE_PRODUCT)" \
		--value "$(VERSION)" --kvs-arn "$(DOCS_REFERENCE_KVS_ARN)" \
		--region us-east-1 --if-match "$$ETAG"

publish-to-maven-local:
	./gradlew :kinegram-emrtd-connector:publishToMavenLocal

clean:
	./gradlew clean
