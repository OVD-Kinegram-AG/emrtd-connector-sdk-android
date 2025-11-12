package com.kinegram.android.emrtdconnectorapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.widget.Button
import android.widget.EditText
import java.util.*
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {
	private lateinit var prefs: SharedPreferences
	private lateinit var canEditText: EditText
	private lateinit var documentNumberEditText: EditText
	private lateinit var dateOfBirthEditText: EditText
	private lateinit var dateOfExpiryEditText: EditText

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		prefs = getPreferences(Context.MODE_PRIVATE)
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

		canEditText.setText(get(CAN_KEY))
		documentNumberEditText.setText(get(DOCUMENT_NUMBER_KEY))
		dateOfBirthEditText.setText(get(DATE_OF_BIRTH_KEY))
		dateOfExpiryEditText.setText(get(DATE_OF_EXPIRY_KEY))
	}

	private fun storePreferredValues() {
		prefs.edit {
			putString(CAN_KEY, canEditText.text.toString())
			putString(DOCUMENT_NUMBER_KEY, documentNumberEditText.text.toString())
			putString(DATE_OF_BIRTH_KEY, dateOfBirthEditText.text.toString())
			putString(DATE_OF_EXPIRY_KEY, dateOfExpiryEditText.text.toString())
		}
	}

	private fun startReadingUsingCAN() {
		val uid = UUID.randomUUID()
		val intent = Intent(this, ReadingActivity::class.java).apply {
			putExtra(VALIDATION_ID_KEY, uid.toString())
			putExtra(CAN_KEY, canEditText.text.toString())
		}
		startActivity(intent)
	}

	private fun startReadingUsingMRZInfo() {
		val uid = UUID.randomUUID()
		val intent = Intent(this, ReadingActivity::class.java).apply {
			putExtra(VALIDATION_ID_KEY, uid.toString())
			putExtra(DOCUMENT_NUMBER_KEY, documentNumberEditText.text.toString())
			putExtra(DATE_OF_BIRTH_KEY, dateOfBirthEditText.text.toString())
			putExtra(DATE_OF_EXPIRY_KEY, dateOfExpiryEditText.text.toString())
		}
		startActivity(intent)
	}

	companion object {
		private const val VALIDATION_ID_KEY = ReadingActivity.VALIDATION_ID_KEY
		private const val CAN_KEY = ReadingActivity.CAN_KEY
		private const val DOCUMENT_NUMBER_KEY = ReadingActivity.DOCUMENT_NUMBER_KEY
		private const val DATE_OF_BIRTH_KEY = ReadingActivity.DATE_OF_BIRTH_KEY
		private const val DATE_OF_EXPIRY_KEY = ReadingActivity.DATE_OF_EXPIRY_KEY
	}
}
