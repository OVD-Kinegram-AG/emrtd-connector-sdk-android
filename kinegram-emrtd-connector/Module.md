# Module Kinegram eMRTD Connector

Enable the Document Validation Server (DocVal Server) to read and verify an eMRTD via a WebSocket
connection.

The DocVal server is able to read the data (like MRZ info or photo of face) and verify the
authenticity and integrity of the data.

If the eMRTD supports the required protocols, the DocVal Server will additionally be able to verify
that the chip was not cloned.

The DocVal Server will post the result to your **Result-Server**.

## Requirements

ℹ️ You **must** provide either the **card access number (CAN)** _or_ the **document number**, the
**date of birth** and the **date of expiry** to access the eMRTD.
Refer to [ICAO Doc 9303 **Part 4**][icao9303].

## Usage Example

1️⃣ Acquire an [IsoDep][iso_dep] from Android.
Refer to the [NFC basics Guide][nfc_basics] from Google on how to configure your app and allow your
Activity to receive the [ACTION_TECH_DISCOVERED][action_tech_discovered] intent.

2️⃣ Enable the DocVal Server to access the eMRTD via an
[EmrtdConnector](com.kinegram.android.emrtdconnector.EmrtdConnector) instance as shown below.

💡[Library Package was changed to "com.kinegram.android.emrtdconnector"](#package-comkinegramandroidemrtdconnector)

```kotlin
// Client ID Functions as an API Access key.
val clientId = "example_client"

// URL of the DocVal Service API Endpoint.
val serverUrl = "wss://kinegramdocval.lkis.de/ws1/validate"

val emrtdConnector = EmrtdConnector(
	clientId, serverUrl, ::closedListener, ::statusListener, ::emrtdPassportListener
)

fun connect() {
	// IsoDep acquired from Android
	val isoDep: IsoDep

	// Access Key values from the MRZ
	val documentNumber = "123456789"
	val dateOfBirth = "970101" // yyMMDD
	val dateOfExpiry = "221212" // yyMMDD

	// Unique transaction ID, usually from your Server
	val validationId = UUID.randomUUID().toString()

	val options = ConnectionOptions.Builder()
		.setValidationId(validationId)
		.setChipAccessKeyFromMrz(documentNumber, dateOfBirth, dateOfExpiry)
		.build()

	emrtdConnector.connect(isoDep, options)
}

fun closedListener(code: Int, reason: String, remote: Boolean) {
	if (code != 1000) {
		println("Session closed because of a problem. Reason: $reason")
	} else {
		println("Session closed.")
	}
}

fun statusListener(status: String) {
	println("Status $status")
}

fun emrtdPassportListener(emrtdPassport: EmrtdPassport?, exception: JSONException) {
	println("Received Result from Server. $emrtdPassport")
}
```

## Session Close Reason values

Read more about the possible Close Reason values in
the [ClosedListener][com.kinegram.android.emrtdconnector.ClosedListener] documentation.

## eMRTD Session Status values

Read more about the possible Status values in the
[StatusListener][com.kinegram.android.emrtdconnector.StatusListener] documentation.


## Library Package was changed to "com.kinegram.android.emrtdconnector"

Update your import statements accordingly.

Version 0.0.14 and before:

```kotlin
import com.kurzdigital.android.emrtdconnector.*
```

Now:

```kotlin
import com.kinegram.android.emrtdconnector.*
```

[icao9303]: https://www.icao.int/publications/pages/publication.aspx?docnum=9303

[iso_dep]: https://developer.android.com/reference/android/nfc/tech/IsoDep

[nfc_basics]: https://developer.android.com/guide/topics/connectivity/nfc/nfc

[action_tech_discovered]: https://developer.android.com/reference/android/nfc/NfcAdapter#ACTION_TECH_DISCOVERED

# Package com.kinegram.android.emrtdconnector

Enable the DocVal Server to access the eMRTD via an
[EmrtdConnector](com.kinegram.android.emrtdconnector.EmrtdConnector) instance.
