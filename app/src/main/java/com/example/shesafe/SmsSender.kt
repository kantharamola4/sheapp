package com.example.shesafe

import android.content.Context
import android.telephony.SmsManager
import android.widget.Toast

object SmsSender {
    fun sendEmergencySMS(context: Context) {
        val numbers = listOf("8374562014", "7659939767")
        val message = "Emergency! My location: https://maps.google.com/?q=17.385044,78.486671"

        try {
            val smsManager = SmsManager.getDefault()
            numbers.forEach { smsManager.sendTextMessage(it, null, message, null, null) }
            Toast.makeText(context, "SMS sent", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send SMS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
