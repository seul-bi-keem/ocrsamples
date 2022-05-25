package com.example.ocrexamples

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSIONS_REQUEST_CAMERA = 101
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CAMERA)
        } else {
            initUI()
        }
    }

    private fun initUI() {
        val mlKitButton = findViewById<AppCompatButton>(R.id.mlkit)
        val cardioButton = findViewById<AppCompatButton>(R.id.cardio)
        val tesseractButton = findViewById<AppCompatButton>(R.id.tesseract)

        mlKitButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, MlkitActivity::class.java))
        }

        cardioButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, CardioActivity::class.java))
        }

        tesseractButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, TesseractActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CAMERA -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initUI()
                } else {
                    Toast.makeText(this, "카메라 권한 없음", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }
}