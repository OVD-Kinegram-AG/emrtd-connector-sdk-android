.PHONY: update-license-report

update-license-report:
	./gradlew :kinegram-emrtd-connector:generateLicenseReport
	# Remove timestamp, trim empty lines and copy the file
	grep -v "This report was generated at" kinegram-emrtd-connector/build/reports/dependency-license/THIRD-PARTY-NOTICES.txt | sed -e :a -e '/^\n*$$/{$$d;N;ba' -e '}' > ./THIRD-PARTY-NOTICES.txt
