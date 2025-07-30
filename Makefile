.PHONY: update-license-report distribution

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

license-report:
	./gradlew :kinegram-emrtd-connector:generateLicenseReport
	# Remove timestamp, trim empty lines and copy the file
	mkdir -p public
	grep -v "This report was generated at" kinegram-emrtd-connector/build/reports/dependency-license/THIRD-PARTY-NOTICES.txt | sed -e :a -e '/^\n*$$/{$$d;N;ba' -e '}' > ./public/THIRD-PARTY-NOTICES.txt

dokka:
	./gradlew :kinegram-emrtd-connector:dokkaHtml :kinegram-emrtd-connector:dokkaJavaDoc
	mkdir -p public
	cp -r kinegram-emrtd-connector/build/dokka/html public/dokka
	cp -r kinegram-emrtd-connector/build/dokka/javadoc public/javadoc

distribution:
	VERSION="$$(grep -m1 '^version[[:space:]]*=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"; \
	echo "Building distribution for version $$VERSION"; \
	sed -i'' -e "s/emrtdconnector = \".*\"/emrtdconnector = \"$$VERSION\"/" \
		distribution/gradle/libs.versions.toml;
	mkdir -p public
	rm -f public/distribution.zip
	(cd distribution && zip -r ../public/distribution.zip .)

pages: dokka license-report distribution
	tools/pages
