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

public class EmrtdConnectorActivity extends AppCompatActivity {
    public final static String CLIENT_ID = "CLIENT_ID";
    public final static String VALIDATION_URI = "VALIDATION_URI";
    public final static String VALIDATION_ID_KEY = "VALIDATION_ID";
    public final static String CAN_KEY = "CAN";
    public final static String DOCUMENT_NUMBER_KEY = "DOCUMENT_NUMBER";
    public final static String DATE_OF_BIRTH_KEY = "DATE_OF_BIRTH";
    public final static String DATE_OF_EXPIRY_KEY = "DATE_OF_EXPIRY";
    public final static String RETURN_DATA = "PASSPORT_DATA";
    public final static String RETURN_ERROR = "JSON_ERROR";

    private final static String TECH_ISO_DEP = "android.nfc.tech.IsoDep";

    private final ClosedListener closedListener = (code, reason, remote) -> finish();
    private final StatusListener statusListener = new StatusListener() {
        @Override
        public void handle(String status) {
            statusTextView.setText(getStatusText(status));
        }
    };
    private final EmrtdPassportListener passportListener = new EmrtdPassportListener() {
        @Override
        public void handle(EmrtdPassport emrtdPassport, JSONException exception) {
            statusTextView.setText(emrtdPassport.toString());

            Intent intent = new Intent();
            intent.putExtra(RETURN_DATA, emrtdPassport);
            if (exception != null) {
                intent.putExtra(RETURN_ERROR, exception);
            }
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private EmrtdConnector emrtdConnector;
    private NfcAdapter nfcAdapter;
    private boolean nfcChipConnected;
    private PendingIntent pendingIntent;

    private String validationId;
    private String can;
    private String documentNumber;
    private String dateOfBirth;
    private String dateOfExpiry;

    private TextView statusTextView;
    private CircularProgressIndicator progressIndicator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emrtd_connector);

        statusTextView = findViewById(R.id.status_textview);
        progressIndicator = findViewById(R.id.progress_indicator);

        Intent intent = getIntent();
        String clientId = intent.getStringExtra(CLIENT_ID);
        String validationUri = intent.getStringExtra(VALIDATION_URI);
        validationId = intent.getStringExtra(VALIDATION_ID_KEY);

        can = intent.getStringExtra(CAN_KEY);

        documentNumber = intent.getStringExtra(DOCUMENT_NUMBER_KEY);
        dateOfBirth = intent.getStringExtra(DATE_OF_BIRTH_KEY);
        dateOfExpiry = intent.getStringExtra(DATE_OF_EXPIRY_KEY);

        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> finish());

        Intent newIntent = new Intent(this, getClass());
        newIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        pendingIntent = PendingIntent.getActivity(this, 50, newIntent, flags);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        try {
            emrtdConnector = new EmrtdConnector(
                clientId,
                validationUri,
                closedListener,
                statusListener,
                passportListener);
        } catch (URISyntaxException e) {
            statusTextView.setText(e.getLocalizedMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, R.string.nfc_unavailable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, new String[][]{
                new String[]{TECH_ISO_DEP}
            });
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);

        if (!NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            statusTextView.setText(String.format(getString(R.string.unexpected_intent_action), intent.getAction()));
            return;
        }

        Tag tag = IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag.class);
        if (tag == null || !Arrays.asList(tag.getTechList()).contains(TECH_ISO_DEP)) {
            statusTextView.setText(String.format(getString(R.string.unexpected_tag), tag));
            return;
        }

        if (nfcChipConnected) {
            statusTextView.setText(getString(R.string.already_connected));
            return;
        }

        IsoDep isoDep = IsoDep.get(tag);
        nfcChipConnected = true;

        ChipAccessKey chipAccessKey;
        if (can != null && !can.isEmpty()) {
            chipAccessKey = new ChipAccessKey.FromCan(can);
        } else {
            chipAccessKey = new ChipAccessKey.FromMrz(
                documentNumber, dateOfBirth, dateOfExpiry);
        }

        ConnectionOptions options = new ConnectionOptions.Builder()
            .setChipAccessKey(chipAccessKey)
            .setValidationId(validationId)
            .build();

        progressIndicator.setVisibility(View.VISIBLE);
        emrtdConnector.connect(isoDep, options);
    }

    private int getStatusText(String status) {
        switch (status) {
            case StatusListener.READ_ATR_INFO:
                return R.string.state_read_atr_info;
            case StatusListener.ACCESS_CONTROL:
                return R.string.state_access_control;
            case StatusListener.READ_SOD:
                return R.string.state_read_sod;
            case StatusListener.READ_DG1:
                return R.string.state_read_dg1;
            case StatusListener.READ_DG2:
                return R.string.state_read_dg2;
            case StatusListener.READ_DG7:
                return R.string.state_read_dg7;
            case StatusListener.READ_DG11:
                return R.string.state_read_dg11;
            case StatusListener.READ_DG12:
                return R.string.state_read_dg12;
            case StatusListener.READ_DG14:
                return R.string.state_read_dg14;
            case StatusListener.READ_DG15:
                return R.string.state_read_dg15;
            case StatusListener.CHIP_AUTHENTICATION:
                return R.string.state_chip_authentication;
            case StatusListener.ACTIVE_AUTHENTICATION:
                return R.string.state_active_authentication;
            case StatusListener.PASSIVE_AUTHENTICATION:
                return R.string.state_passive_authentication;
            case StatusListener.DONE:
                return R.string.state_done;
            default:
                return R.string.state_connecting_to_server;
        }
    }
}
