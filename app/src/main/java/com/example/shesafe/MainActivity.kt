package com.example.shesafe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val REQUEST_LOCATION_PERMISSION = 1001
    private val numbers = listOf("8374562014", "7659939767")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sosButton: Button = findViewById(R.id.emergencyBtn)

        sosButton.setOnClickListener {
            if (checkPermissions()) {
                fetchAndSendLocation()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.SEND_SMS
                    ),
                    REQUEST_LOCATION_PERMISSION
                )
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            fetchAndSendLocation() // optional: you can also wait for user to press SOS again
        } else {
            Toast.makeText(this, "All permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAndSendLocation() {
        LocationHelper.getCurrentLocation(this) { location ->
            if (location != null) {
                val message = "I need help! My current location is: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                sendSMS(message)
            } else {
                Toast.makeText(this, "Could not fetch location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendSMS(message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            for (number in numbers) {
                smsManager.sendTextMessage(number, null, message, null, null)
            }
            Toast.makeText(this, "SMS sent successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
