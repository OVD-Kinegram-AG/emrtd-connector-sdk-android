package com.kinegram.android.emrtdconnector;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONException;

import java.net.URISyntaxException;
import java.util.Arrays;

public class EmrtdConnectorActivity extends AppCompatActivity implements ClosedListener, StatusListener, EmrtdPassportListener {
	private final static String TECH_ISO_DEP = "android.nfc.tech.IsoDep";

	private final static String CLIENT_ID = "CLIENT_ID";
	private final static String VALIDATION_URI = "VALIDATION_URI";
	private final static String VALIDATION_ID_KEY = "VALIDATION_ID";
	private final static String CAN_KEY = "CAN";
	private final static String DOCUMENT_NUMBER_KEY = "DOCUMENT_NUMBER";
	private final static String DATE_OF_BIRTH_KEY = "DATE_OF_BIRTH";
	private final static String DATE_OF_EXPIRY_KEY = "DATE_OF_EXPIRY";

	private final static String RETURN_DATA = "PASSPORT_DATA";
	private final static String RETURN_ERROR = "JSON_ERROR";

	private EmrtdConnector _emrtdConnector;
	private NfcAdapter _nfcAdapter;
	private boolean _nfcChipConnected;
	private PendingIntent _pendingIntent;

	private String _validationId;
	private String _can;
	private String _documentNumber;
	private String _dateOfBirth;
	private String _dateOfExpiry;

	private TextView _statusTextView;
	private CircularProgressIndicator _progressIndicator;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_emrtd_connector);

		_statusTextView = findViewById(R.id.status_textview);
		_progressIndicator = findViewById(R.id.progress_indicator);

		String clientId = getIntent().getStringExtra(CLIENT_ID);
		String validationUri = getIntent().getStringExtra(VALIDATION_URI);
		_validationId = getIntent().getStringExtra(VALIDATION_ID_KEY);

		_can = getIntent().getStringExtra(CAN_KEY);

		_documentNumber = getIntent().getStringExtra(DOCUMENT_NUMBER_KEY);
		_dateOfBirth = getIntent().getStringExtra(DATE_OF_BIRTH_KEY);
		_dateOfExpiry = getIntent().getStringExtra(DATE_OF_EXPIRY_KEY);

		Button cancelButton = findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(v -> finish());

		Intent intent = new Intent(this, getClass());
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		int flags;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
		} else {
			flags = PendingIntent.FLAG_UPDATE_CURRENT;
		}

		_pendingIntent = PendingIntent.getActivity(this, 50, intent, flags);
		_nfcAdapter = NfcAdapter.getDefaultAdapter(this);

		try {
			_emrtdConnector = new EmrtdConnector(clientId, validationUri, this, this, this);
		} catch (URISyntaxException e) {
			_statusTextView.setText(e.getLocalizedMessage());
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!_nfcAdapter.isEnabled()) {
			Toast.makeText(this, R.string.nfc_unavailable, Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		if (_nfcAdapter != null) {
			_nfcAdapter.enableForegroundDispatch(this, _pendingIntent, null, new String[][]{
				new String[]{TECH_ISO_DEP}
			});
		}
	}

	@Override
	protected void onNewIntent(@NonNull Intent intent) {
		super.onNewIntent(intent);

		if (!NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
			_statusTextView.setText(String.format(getString(R.string.unexpected_intent_action), intent.getAction()));
			return;
		}

		Tag tag = IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag.class);
		if (tag == null || !Arrays.asList(tag.getTechList()).contains(TECH_ISO_DEP)) {
			_statusTextView.setText(String.format(getString(R.string.unexpected_tag), tag));
			return;
		}

		if (_nfcChipConnected) {
			_statusTextView.setText(getString(R.string.already_connected));
			return;
		}

		IsoDep isoDep = IsoDep.get(tag);
		_nfcChipConnected = true;

		ChipAccessKey chipAccessKey;
		if (_can != null && !_can.isEmpty()) {
			chipAccessKey = new ChipAccessKey.FromCan(_can);
		} else {
			chipAccessKey = new ChipAccessKey.FromMrz(
				_documentNumber, _dateOfBirth, _dateOfExpiry);
		}

		ConnectionOptions options = new ConnectionOptions.Builder()
			.setChipAccessKey(chipAccessKey)
			.setValidationId(_validationId)
			.build();

		_progressIndicator.setVisibility(View.VISIBLE);
		_emrtdConnector.connect(isoDep, options);
	}

	// ClosedListener
	@Override
	public void handle(int code, String reason, boolean remote) {
		finish();
	}

	// EmrtdPassportListener
	@Override
	public void handle(EmrtdPassport emrtdPassport, JSONException exception) {
		_statusTextView.setText(emrtdPassport.toString());

		Intent intent = new Intent();
		intent.putExtra(RETURN_DATA, emrtdPassport);
		if (exception != null) {
			intent.putExtra(RETURN_ERROR, exception);
		}
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	// StatusListener
	@Override
	public void handle(String status) {
		_statusTextView.setText(status);
	}
}
