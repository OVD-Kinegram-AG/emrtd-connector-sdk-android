package com.kinegram.android.emrtdconnector;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.kinegram.emrtd.DataGroupSet;

import org.json.JSONException;

import java.net.URISyntaxException;
import java.util.Arrays;

public class EmrtdConnectorActivity extends AppCompatActivity {
    public final static String CLIENT_ID = "CLIENT_ID";
    public final static String VALIDATION_URI = "VALIDATION_URI";
    public final static String VALIDATION_ID = "VALIDATION_ID";
    public final static String CAN = "CAN";
    public final static String DOCUMENT_NUMBER = "DOCUMENT_NUMBER";
    public final static String DATE_OF_BIRTH = "DATE_OF_BIRTH";
    public final static String DATE_OF_EXPIRY = "DATE_OF_EXPIRY";
    /**
     * Optional {@code int[]} extra holding the numbers of the data groups to
     * read, for example {@code {1, 2}} for the MRZ and the face image. Without
     * it the read uses {@link DataGroupSet#STANDARD}.
     */
    public final static String DATA_GROUPS = "DATA_GROUPS";
    public final static String RETURN_DATA = "DATA";
    public final static String RETURN_ERROR = "ERROR";

    private final static String TECH_ISO_DEP = "android.nfc.tech.IsoDep";

    private final ClosedListener closedListener = (code, reason, remote) -> finish();
    private final StatusListener statusListener = new StatusListener() {
        @Override
        public void handle(String status) {
            statusTextView.setText(getStatusText(status));
        }
    };
    private final EmrtdPassportListener passportListener = (emrtdPassport, exception) -> {
        if (exception != null) {
            returnError(exception.toString());
            return;
        }

        String json;
        try {
            json = emrtdPassport.toJSON().toString();
        } catch (JSONException e) {
            returnError(e.getLocalizedMessage());
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(RETURN_DATA, json);
        setResult(Activity.RESULT_OK, intent);
        finish();
    };

    private EmrtdConnector emrtdConnector;
    private NfcAdapter nfcAdapter;
    private boolean nfcChipConnected;

    private String validationId;
    private String can;
    private String documentNumber;
    private String dateOfBirth;
    private String dateOfExpiry;
    private DataGroupSet filesToRead;

    private TextView statusTextView;
    private CircularProgressIndicator progressIndicator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emrtd_connector);

        statusTextView = findViewById(R.id.status_textview);
        progressIndicator = findViewById(R.id.progress_indicator);

        Intent intent = getIntent();
        if (intent == null) {
            returnError("Could not get intent");
            return;
        }

        String clientId = intent.getStringExtra(CLIENT_ID);
        String validationUri = intent.getStringExtra(VALIDATION_URI);
        validationId = intent.getStringExtra(VALIDATION_ID);
        if (clientId == null || clientId.isEmpty() ||
                validationUri == null || validationUri.isEmpty()
        ) {
            returnError("clientId or validationUri is null or empty");
            return;
        }

        can = intent.getStringExtra(CAN);

        documentNumber = intent.getStringExtra(DOCUMENT_NUMBER);
        dateOfBirth = intent.getStringExtra(DATE_OF_BIRTH);
        dateOfExpiry = intent.getStringExtra(DATE_OF_EXPIRY);

        int[] dataGroups = intent.getIntArrayExtra(DATA_GROUPS);
        try {
            filesToRead = dataGroups == null
                    ? DataGroupSet.STANDARD
                    : DataGroupSet.of(dataGroups);
        } catch (IllegalArgumentException e) {
            returnError(e.getLocalizedMessage());
            return;
        }

        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> finish());

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
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            returnError(getString(R.string.nfc_unavailable));
            return;
        }

        Bundle options = new Bundle();
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 50);
        nfcAdapter.enableReaderMode(
                this,
                tag -> {
                    runOnUiThread(() -> handleTag(tag));
                },
                NfcAdapter.FLAG_READER_NFC_A |
                        NfcAdapter.FLAG_READER_NFC_B |
                        NfcAdapter.FLAG_READER_NFC_F |
                        NfcAdapter.FLAG_READER_NFC_V |
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                options
        );
    }

    @Override
    protected void onPause() {
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }
        super.onPause();
    }

    private void handleTag(Tag tag) {
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
                .setFilesToRead(filesToRead)
                .build();

        progressIndicator.setVisibility(View.VISIBLE);
        try {
            emrtdConnector.connect(isoDep, options);
        } catch (Exception e) {
            returnError(e.getLocalizedMessage());
        }
    }

    private void returnError(String message) {
        Intent intent = new Intent();
        intent.putExtra(RETURN_ERROR, message);
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
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
            case StatusListener.READ_DG3:
                return R.string.state_read_dg3;
            case StatusListener.READ_DG4:
                return R.string.state_read_dg4;
            case StatusListener.READ_DG5:
                return R.string.state_read_dg5;
            case StatusListener.READ_DG6:
                return R.string.state_read_dg6;
            case StatusListener.READ_DG8:
                return R.string.state_read_dg8;
            case StatusListener.READ_DG9:
                return R.string.state_read_dg9;
            case StatusListener.READ_DG10:
                return R.string.state_read_dg10;
            case StatusListener.READ_DG13:
                return R.string.state_read_dg13;
            case StatusListener.READ_DG16:
                return R.string.state_read_dg16;
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
