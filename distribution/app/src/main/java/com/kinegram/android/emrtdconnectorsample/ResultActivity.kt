package com.kinegram.android.emrtdconnectorsample

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.TextUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kinegram.android.emrtdconnector.EmrtdPassport
import java.text.SimpleDateFormat
import java.util.*

class ResultActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_result)
		val resultTextview = findViewById<TextView>(R.id.result_textview)
		val emrtd = intent.getParcelableExtra<EmrtdPassport>(RESULT_KEY)
		requireNotNull(emrtd)

		val passiveAuthenticationDescriptionResId = if (emrtd.passiveAuthentication) {
			R.string.passive_authentication_success
		} else {
			R.string.passive_authentication_failed
		}

		val cloneDescriptionResId = listOf(emrtd.activeAuthenticationResult, emrtd.chipAuthenticationResult).let {
			when {
				EmrtdPassport.CheckResult.FAILED in it -> R.string.chip_is_cloned
				EmrtdPassport.CheckResult.SUCCESS in it -> R.string.chip_is_not_cloned
				else -> 0
			}
		}

		BitmapDrawable(resources, emrtd.facePhoto.inputStream()).takeIf { it.bitmap != null }?.let {
			// Display the face photo
			resultTextview.setCompoundDrawablesWithIntrinsicBounds(null, it, null, null)
		}

		resultTextview.text = buildString {
			if (emrtd.isExpired) {
				append(getString(R.string.document_is_expired), "\n\n")
			}
			append(getString(passiveAuthenticationDescriptionResId), "\n\n")
			if (cloneDescriptionResId != 0) {
				append(getString(cloneDescriptionResId), "\n\n")
			}
			append(emrtd.mrzInfo)
			if (!emrtd.errors.isNullOrEmpty()) {
				append("\n\n", TextUtils.join(", ", emrtd.errors))
			}
		}
	}

	companion object {
		const val RESULT_KEY = "RESULT"

		private val yyMMddFormat = SimpleDateFormat("yyMMdd", Locale.US).apply {
			val twoDigitStartDate = Calendar.getInstance().apply {
				time = Date()
				add(Calendar.YEAR, -40)
			}.time
			set2DigitYearStart(twoDigitStartDate)
		}

		private val EmrtdPassport.isExpired
			get() = mrzInfo?.dateOfExpiry?.let { Date().after(yyMMddFormat.parse(it)) } == true
	}
}
