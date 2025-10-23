package com.example.shesafe

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.telephony.SmsManager
import android.widget.Toast

class PanicService : Service() {

    private val numbers = listOf("8374562014", "7659939767")

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LocationHelper.getCurrentLocation(this) { location ->
            if (location != null) {
                val message = "🚨 Emergency! Location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                sendSMS(message)
            } else {
                // Send SMS without location as fallback
                val fallbackMessage = "🚨 Emergency! Help needed! (Location unavailable - please call me)"
                sendSMS(fallbackMessage)
            }
        }
        return START_NOT_STICKY
    }


    private fun sendSMS(message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            for (number in numbers) {
                smsManager.sendTextMessage(number, null, message, null, null)
            }
            Toast.makeText(this, "Emergency SMS sent", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "SMS failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
