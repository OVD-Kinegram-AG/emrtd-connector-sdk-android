.PHONY: update-license-report

PACKAGE = com.kinegram.android.emrtdconnectorapp

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
	./gradlew :kinegram-emrtd-connector:dokkaHtml :kinegram-emrtd-connector:dokkaJavaDoc
	mkdir -p _site
	cp -r kinegram-emrtd-connector/build/dokka/html _site/dokka
	cp -r kinegram-emrtd-connector/build/dokka/javadoc _site/javadoc

pages: dokka
	tools/pages

publish-to-maven-local:
	./gradlew :kinegram-emrtd-connector:publishToMavenLocal
