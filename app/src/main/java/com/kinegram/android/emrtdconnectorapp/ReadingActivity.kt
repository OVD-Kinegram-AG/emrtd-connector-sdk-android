package com.kinegram.android.emrtdconnectorapp

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.kinegram.android.emrtdconnector.ChipAccessKey
import com.kinegram.android.emrtdconnector.ClosedListener
import com.kinegram.android.emrtdconnector.ConnectionOptions
import com.kinegram.android.emrtdconnector.EmrtdConnector
import com.kinegram.android.emrtdconnector.EmrtdConnectorActivity
import com.kinegram.android.emrtdconnector.EmrtdPassport
import com.kinegram.android.emrtdconnector.StatusListener

class ReadingActivity : AppCompatActivity() {
    private lateinit var statusTextView: TextView
    private lateinit var errorTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cancelButton: Button
    private lateinit var showResultButton: Button
    private lateinit var doneButton: Button

    private lateinit var validationId: String
    private lateinit var can: String
    private lateinit var documentNumber: String
    private lateinit var dateOfBirth: String
    private lateinit var dateOfExpiry: String

    private lateinit var pendingIntent: PendingIntent
    private lateinit var emrtdConnector: EmrtdConnector
    private var nfcAdapter: NfcAdapter? = null
    private var nfcChipConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reading)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        statusTextView = findViewById(R.id.status_textview)
        progressBar = findViewById(R.id.progressbar)
        errorTextView = findViewById(R.id.error_textview)
        cancelButton = findViewById(R.id.cancel_button)
        showResultButton = findViewById(R.id.show_result_button)
        doneButton = findViewById(R.id.done_button)

        val clientId = intent.getStringExtra(EmrtdConnectorActivity.CLIENT_ID) ?: ""
        val validationUri = intent.getStringExtra(EmrtdConnectorActivity.VALIDATION_URI) ?: ""
        validationId = intent.getStringExtra(EmrtdConnectorActivity.VALIDATION_ID) ?: ""
        can = intent.getStringExtra(EmrtdConnectorActivity.CAN) ?: ""
        documentNumber = intent.getStringExtra(EmrtdConnectorActivity.DOCUMENT_NUMBER) ?: ""
        dateOfBirth = intent.getStringExtra(EmrtdConnectorActivity.DATE_OF_BIRTH) ?: ""
        dateOfExpiry = intent.getStringExtra(EmrtdConnectorActivity.DATE_OF_EXPIRY) ?: ""

        emrtdConnector = EmrtdConnector(
            clientId,
            validationUri,
            ::closedListener,
            ::statusListener,
            ::emrtdPassportListener
        )
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        pendingIntent = PendingIntent.getActivity(this, 50, intent, flags)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        cancelButton.setOnClickListener { emrtdConnector.cancel() }
        doneButton.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter?.isEnabled != true) {
            toast(R.string.nfc_unavailable)
            finish()
            return
        }
        nfcAdapter?.enableForegroundDispatch(
            this, pendingIntent, null, arrayOf(arrayOf(TECH_ISO_DEP))
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED != intent.action) {
            println("Unexpected Intent Action ${intent.action}")
            return
        }
        val tag = IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag::class.java)
        if (tag == null || !tag.techList.contains(TECH_ISO_DEP)) {
            println("Unexpected tag $tag")
            return
        }
        if (nfcChipConnected) {
            println("Already connected to a nfc chip.")
            return
        }

        val isoDep = IsoDep.get(tag)
        nfcChipConnected = true
        progressBar.visibility = View.VISIBLE
        cancelButton.isEnabled = true
        cancelButton.visibility = View.VISIBLE

        val chipAccessKey = if (can.isNotEmpty()) {
            ChipAccessKey.FromCan(can)
        } else {
            ChipAccessKey.FromMrz(documentNumber, dateOfBirth, dateOfExpiry)
        }

        val options = ConnectionOptions.Builder()
            .setValidationId(validationId)
            .setChipAccessKey(chipAccessKey)
            .build()

        emrtdConnector.connect(isoDep, options)
    }

    @SuppressLint("SetTextI18n")
    private fun statusListener(status: String) {
        val s = status.replace("_", " ")
        statusTextView.text = "${statusTextView.text}\n$s".trim()

        // These are the status values to expect
        when (status) {
            StatusListener.CONNECTING_TO_SERVER -> {}
            StatusListener.READ_ATR_INFO -> {}
            StatusListener.ACCESS_CONTROL -> {}
            StatusListener.READ_SOD -> {}
            StatusListener.READ_DG14 -> {}
            StatusListener.CHIP_AUTHENTICATION -> {}
            StatusListener.READ_DG15 -> {}
            StatusListener.ACTIVE_AUTHENTICATION -> {}
            StatusListener.READ_DG1 -> {}
            StatusListener.READ_DG2 -> {}
            StatusListener.READ_DG7 -> {}
            StatusListener.READ_DG11 -> {}
            StatusListener.READ_DG12 -> {}
            StatusListener.PASSIVE_AUTHENTICATION -> {}
            StatusListener.DONE -> {}
        }
    }

    private fun emrtdPassportListener(emrtdPassport: EmrtdPassport?, e: Exception?) {
        if (e != null) {
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
            return;
        }
        showResultButton.setOnClickListener {
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra(ResultActivity.RESULT_KEY, emrtdPassport)
            }
            startActivity(intent)
            finish()
        }
        showResultButton.visibility = View.VISIBLE
    }

    private fun closedListener(code: Int, reason: String, remote: Boolean) {
        progressBar.visibility = View.GONE
        cancelButton.isEnabled = false
        cancelButton.visibility = View.INVISIBLE
        if (showResultButton.visibility != View.VISIBLE) {
            doneButton.visibility = View.VISIBLE
        }

        if (code != 1000) {
            errorTextView.visibility = View.VISIBLE
            val sId = if (remote) {
                R.string.closed_by_remote_endpoint_message
            } else {
                R.string.closed_by_this_endpoint_message
            }
            val reasonPhrase = reason.replace("_", " ")
            errorTextView.text = getString(sId, code, reasonPhrase)

            // These are the close reasons to expect,
            // if the Close Code is not 1000
            when (reason) {
                ClosedListener.TIMEOUT_WHILE_WAITING_FOR_START_MESSAGE -> {}
                ClosedListener.TIMEOUT_WHILE_WAITING_FOR_RESPONSE -> {}
                ClosedListener.MAX_SESSION_TIME_EXCEEDED -> {}
                ClosedListener.UNEXPECTED_MESSAGE -> {}
                ClosedListener.PROTOCOL_ERROR -> {}
                ClosedListener.INVALID_CLIENT_ID -> {}
                ClosedListener.INVALID_ACCESS_KEY_VALUES,
                ClosedListener.ACCESS_CONTROL_FAILED -> {
                    toast(R.string.check_access_key)
                }

                ClosedListener.COMMUNICATION_FAILED -> {
                    toast(R.string.check_connection)
                    if (!remote) {
                        // Query the exact exception that occurred during a WebSocket operation
                        val webSocketException = emrtdConnector.webSocketClientException
                    }
                }

                ClosedListener.FILE_READ_ERROR -> {}
                ClosedListener.EMRTD_PASSPORT_READER_ERROR -> {}
                ClosedListener.SERVER_ERROR -> {}
                ClosedListener.POST_TO_RESULT_SERVER_FAILED -> {
                    toast(R.string.post_to_result_server_failed)
                }

                ClosedListener.NFC_CHIP_COMMUNICATION_FAILED -> {
                    toast(R.string.nfc_communication_failed)
                    if (!remote) {
                        // Query the exact exception that occurred during an IsoDep Operation
                        val nfcException = emrtdConnector.nfcException
                    }
                }

                ClosedListener.CANCELLED_BY_USER -> {}
            }
        }
    }

    private fun toast(id: Int) {
        Toast.makeText(this, id, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TECH_ISO_DEP = "android.nfc.tech.IsoDep"
    }
}
