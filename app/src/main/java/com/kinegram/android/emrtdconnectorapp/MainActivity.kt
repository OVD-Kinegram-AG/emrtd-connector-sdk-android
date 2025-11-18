package com.kinegram.android.emrtdconnectorapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputFilter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.kinegram.android.emrtdconnector.EmrtdConnectorActivity
import com.kinegram.android.emrtdconnector.EmrtdPassport
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            return@registerForActivityResult
        }
        val passport = result.data?.getParcelableExtra<EmrtdPassport>(
            EmrtdConnectorActivity.RETURN_DATA
        ) ?: return@registerForActivityResult
        startActivity(Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.RESULT_KEY, passport)
        })
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var returnResultCheckbox: CheckBox
    private lateinit var canEditText: EditText
    private lateinit var documentNumberEditText: EditText
    private lateinit var dateOfBirthEditText: EditText
    private lateinit var dateOfExpiryEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getPreferences(MODE_PRIVATE)

        returnResultCheckbox = findViewById(R.id.return_result_only)
        canEditText = findViewById(R.id.can)
        documentNumberEditText = findViewById(R.id.document_number)
        dateOfBirthEditText = findViewById(R.id.date_of_birth)
        dateOfExpiryEditText = findViewById(R.id.date_of_expiry)
        documentNumberEditText.filters = arrayOf(InputFilter.AllCaps())

        restorePreferredValues()

        findViewById<Button>(R.id.start_reading_using_can_button)
            .setOnClickListener {
                startReadingUsingCAN()
            }

        findViewById<Button>(R.id.start_reading_using_mrz_button)
            .setOnClickListener {
                startReadingUsingMRZInfo()
            }
    }

    override fun onPause() {
        super.onPause()
        storePreferredValues()
    }

    private fun restorePreferredValues() {
        fun get(key: String): String {
            return prefs.getString(key, "") ?: ""
        }

        returnResultCheckbox.isChecked = prefs.getBoolean(RETURN_RESULT_KEY, false)
        canEditText.setText(get(EmrtdConnectorActivity.CAN_KEY))
        documentNumberEditText.setText(get(EmrtdConnectorActivity.DOCUMENT_NUMBER))
        dateOfBirthEditText.setText(get(EmrtdConnectorActivity.DATE_OF_BIRTH))
        dateOfExpiryEditText.setText(get(EmrtdConnectorActivity.DATE_OF_EXPIRY))
    }

    private fun storePreferredValues() {
        prefs.edit {
            putBoolean(RETURN_RESULT_KEY, returnResultCheckbox.isChecked)
            putString(EmrtdConnectorActivity.CAN_KEY, canEditText.text.toString())
            putString(
                EmrtdConnectorActivity.DOCUMENT_NUMBER,
                documentNumberEditText.text.toString()
            )
            putString(EmrtdConnectorActivity.DATE_OF_BIRTH, dateOfBirthEditText.text.toString())
            putString(
                EmrtdConnectorActivity.DATE_OF_EXPIRY,
                dateOfExpiryEditText.text.toString()
            )
        }
    }

    private fun startReadingUsingCAN() {
        getTargetIntent().apply {
            putExtra(EmrtdConnectorActivity.VALIDATION_ID, getUUIDString())
            putExtra(EmrtdConnectorActivity.CAN_KEY, canEditText.text.toString())
        }.start()
    }

    private fun startReadingUsingMRZInfo() {
        getTargetIntent().apply {
            putExtra(EmrtdConnectorActivity.VALIDATION_ID, getUUIDString())
            putExtra(
                EmrtdConnectorActivity.DOCUMENT_NUMBER,
                documentNumberEditText.text.toString()
            )
            putExtra(
                EmrtdConnectorActivity.DATE_OF_BIRTH,
                dateOfBirthEditText.text.toString()
            )
            putExtra(
                EmrtdConnectorActivity.DATE_OF_EXPIRY,
                dateOfExpiryEditText.text.toString()
            )
        }.start()
    }

    private fun Intent.start() {
        if (returnResultCheckbox.isChecked) {
            resultLauncher.launch(this)
        } else {
            startActivity(this)
        }
    }

    private fun getTargetIntent() = Intent(
        this,
        if (returnResultCheckbox.isChecked) {
            EmrtdConnectorActivity::class.java
        } else {
            ReadingActivity::class.java
        }
    ).apply {
        putExtra(
            EmrtdConnectorActivity.CLIENT_ID,
            "example_client"
        )
        putExtra(
            EmrtdConnectorActivity.VALIDATION_URI,
            "wss://docval.kurzdigital.com/ws2/validate"
        )
    }

    private fun getUUIDString() = UUID.randomUUID().toString()

    companion object {
        private const val RETURN_RESULT_KEY = "return_result_key"
    }
}
