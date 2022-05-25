package com.example.ocrexamples

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import io.card.payment.CardIOActivity
import io.card.payment.CreditCard

class CardioActivity : AppCompatActivity() {

    private lateinit var resultImage: ImageView
    private lateinit var resultCardTypeImage: ImageView
    private lateinit var resultLabel: TextView

    companion object {
        const val TAG = "CardioActivity"
    }

    private val activityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            var outStr = String()
            var cardTypeImage: Bitmap? = null
            if (it.data?.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT) == true) {

                val result: CreditCard? = it.data!!.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT)
                if (result != null) {
                    outStr += "Card number: ${result.redactedCardNumber}"
                }
            }
            val card = CardIOActivity.getCapturedCardImage(it.data)
            resultImage.setImageBitmap(card)
            resultCardTypeImage.setImageBitmap(cardTypeImage)
            Log.i(TAG, "Set result: $outStr")
            resultLabel.text = outStr
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cardio)

        resultImage = findViewById(R.id.result_image)
        resultCardTypeImage = findViewById(R.id.result_type_image)
        resultLabel = findViewById(R.id.result_label)

        val intent = Intent(this, CardIOActivity::class.java)
        intent.apply {
            putExtra(CardIOActivity.EXTRA_NO_CAMERA, false)
            putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, false)
            putExtra(CardIOActivity.EXTRA_SCAN_EXPIRY, false)
            putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, false)
            putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false)
            putExtra(CardIOActivity.EXTRA_RESTRICT_POSTAL_CODE_TO_NUMERIC_ONLY, false)
            putExtra(CardIOActivity.EXTRA_REQUIRE_CARDHOLDER_NAME,false)
            putExtra(CardIOActivity.EXTRA_SUPPRESS_MANUAL_ENTRY, false)
            putExtra(CardIOActivity.EXTRA_USE_CARDIO_LOGO, false)
            putExtra(CardIOActivity.EXTRA_LANGUAGE_OR_LOCALE, "en")
            putExtra(CardIOActivity.EXTRA_USE_PAYPAL_ACTIONBAR_ICON, false)
            putExtra(CardIOActivity.EXTRA_KEEP_APPLICATION_THEME, true)
            putExtra(CardIOActivity.EXTRA_GUIDE_COLOR, Color.RED)
            putExtra(CardIOActivity.EXTRA_SUPPRESS_CONFIRMATION, false)
            putExtra(CardIOActivity.EXTRA_SUPPRESS_SCAN, false)
            putExtra(CardIOActivity.EXTRA_RETURN_CARD_IMAGE, true);
        }

        activityLauncher.launch(intent)
    }
}