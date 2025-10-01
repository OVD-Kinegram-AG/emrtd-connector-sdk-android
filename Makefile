.PHONY: update-license-report

PACKAGE = com.kinegram.android.emrtdconnectorapp

all: debug install start

debug:
	./gradlew assembleDebug

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
	# Remove timestamp, trim empty lines and copy the file
	grep -v "This report was generated at" kinegram-emrtd-connector/build/reports/dependency-license/THIRD-PARTY-NOTICES.txt | sed -e :a -e '/^\n*$$/{$$d;N;ba' -e '}' > ./THIRD-PARTY-NOTICES.txt

dokka:
	./gradlew :kinegram-emrtd-connector:dokkaHtml :kinegram-emrtd-connector:dokkaJavaDoc
	mkdir -p _site
	cp -r kinegram-emrtd-connector/build/dokka/html _site/dokka
	cp -r kinegram-emrtd-connector/build/dokka/javadoc _site/javadoc

pages: dokka
	tools/pages

publish-to-maven-local:
	./gradlew :kinegram-emrtd-connector:publishToMavenLocal
