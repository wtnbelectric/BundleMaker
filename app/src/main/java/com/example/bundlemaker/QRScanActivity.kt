package com.example.bundlemaker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator

class QRScanActivity : AppCompatActivity() {
    private lateinit var resultText: TextView
    private lateinit var manualInput: EditText
    private lateinit var okButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scan)

        resultText = findViewById(R.id.scan_result_text)
        manualInput = findViewById(R.id.manual_input)
        okButton = findViewById(R.id.ok_button)
        cancelButton = findViewById(R.id.cancel_button)

        // QRスキャン開始
        IntentIntegrator(this).initiateScan()

        okButton.setOnClickListener {
            val result = manualInput.text.toString().ifBlank { resultText.text.toString() }
            val intent = Intent().apply {
                putExtra("scan_result", result)
                putExtra("success", result.isNotBlank())
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                resultText.text = result.contents
                manualInput.setText("")
            } else {
                resultText.text = "QRコードを読み取れませんでした。手動で入力してください。"
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}

