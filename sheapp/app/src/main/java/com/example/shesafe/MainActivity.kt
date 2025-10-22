package com.example.shesafe

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.telephony.SmsManager
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.shesafe.EmotionAnalyzer
import com.example.shesafe.DistressDetector
import com.example.shesafe.FaceVerifier
import com.example.shesafe.HotwordDetector

class MainActivity : AppCompatActivity() {

    private val REQUEST_LOCATION_PERMISSION = 1001
    private val REQUEST_CAMERA = 101
    private lateinit var videoView: VideoView
    private lateinit var emergencyBtn: Button
    //private lateinit var cameraTriggerBtn: Button
    //private lateinit var audioTriggerBtn: Button
    private lateinit var profileBtn: Button
    private lateinit var contactsBtn: Button
    private lateinit var detectEmotionBtn: Button
    private lateinit var mlStatusText: TextView
    
    // ML Components
    private lateinit var distressDetector: DistressDetector
    private lateinit var hotwordDetector: HotwordDetector


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Button references (must come first!)
        emergencyBtn = findViewById(R.id.emergencyBtn)
        //cameraTriggerBtn = findViewById(R.id.cameraTriggerBtn)
        //audioTriggerBtn = findViewById(R.id.audioTriggerBtn)
        profileBtn = findViewById(R.id.profileBtn)
        contactsBtn = findViewById(R.id.contactsBtn)
        detectEmotionBtn = findViewById(R.id.detectEmotionBtn)
        mlStatusText = findViewById(R.id.mlStatusText)

        // Video Background Setup
        videoView = findViewById(R.id.videoBackground)
        val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.background}")
        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)
            videoView.start()
            fadeInButtons()
        }

        // Button click listeners
        emergencyBtn.setOnClickListener {
            if (checkPermissions()) {
                fetchAndSendLocation()
            } else {
                requestPermissions()
            }
        }

//        cameraTriggerBtn.setOnClickListener {
//            openCameraAndTriggerSOS()
//        }
//
//        audioTriggerBtn.setOnClickListener {
//            Toast.makeText(this, "Audio trigger under development", Toast.LENGTH_SHORT).show()
//        }

        profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        contactsBtn.setOnClickListener {
            startActivity(Intent(this, EmergencyContactsActivity::class.java))
        }

        // Open Settings on long-press of profile for demo
        profileBtn.setOnLongClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }

        // Initialize ML Components
        initializeMLComponents()
        
        // Start background safety monitoring
        try { startService(Intent(this, SafetyMonitorService::class.java)) } catch (_: Exception) {}
        detectEmotionBtn.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show()
                    AudioRecorder.startRecording(this)
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    Toast.makeText(this, "Recording stopped. Analyzing emotion...", Toast.LENGTH_SHORT).show()
                    AudioRecorder.stopRecordingAndAnalyze(this) { emotion ->
                        runOnUiThread {
                            Toast.makeText(this, "Detected emotion: $emotion", Toast.LENGTH_LONG).show()
                            
                            // Check for distress indicators
                            val distressEmotions = listOf("fear", "angry", "sad")
                            if (distressEmotions.any { emotion.equals(it, ignoreCase = true) }) {
                                Toast.makeText(this, "Distress detected! Sending SOS...", Toast.LENGTH_SHORT).show()
                                if (checkPermissions()) {
                                    fetchAndSendLocation()
                                } else {
                                    requestPermissions()
                                }
                            }
                        }
                    }
                    true
                }
                else -> false
            }
        }



    }

    override fun onResume() {
        super.onResume()
        videoView.start()
    }

    override fun onPause() {
        super.onPause()
        videoView.pause()
    }

    // Smooth fade-in animation for buttons
    private fun fadeInButtons() {
        val buttons = listOf(emergencyBtn, profileBtn, contactsBtn)
        val anim = AlphaAnimation(0f, 1f).apply {
            duration = 1200
            fillAfter = true
        }
        buttons.forEach {
            it.visibility = View.VISIBLE
            it.startAnimation(anim)
        }
    }

    // Full-screen immersive UI
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.let {
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        )
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ),
            REQUEST_LOCATION_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            fetchAndSendLocation()
        } else {
            Toast.makeText(this, "Permissions are required", Toast.LENGTH_SHORT).show()
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

    private fun openCameraAndTriggerSOS() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(intent, REQUEST_CAMERA)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            fetchAndSendLocation() // Trigger SOS after camera
        }
    }

    private fun getEmergencyNumbers(): List<String> {
        val stored = getSharedPreferences("emergency_contacts", MODE_PRIVATE)
            .getString("contacts", "") ?: ""
        val numbers = stored.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return if (numbers.isEmpty()) listOf("8374562014", "7659939767") else numbers
    }

    private fun sendSMS(message: String) {
        val numbers = getEmergencyNumbers()
        if (numbers.isEmpty()) {
            Toast.makeText(this, "No emergency contacts set", Toast.LENGTH_SHORT).show()
            return
        }
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
    
    private fun initializeMLComponents() {
        updateMLStatus("🤖 ML Systems: Initializing...")
        
        try {
            // Initialize Emotion Analyzer
            EmotionAnalyzer.initialize(this)
            updateMLStatus("🤖 ML Systems: Emotion Analyzer ✓")
            
            // Initialize Distress Detector
            distressDetector = DistressDetector()
            distressDetector.start {
                runOnUiThread {
                    Toast.makeText(this, "Distress detected! Triggering emergency response...", Toast.LENGTH_LONG).show()
                    if (checkPermissions()) {
                        fetchAndSendLocation()
                    } else {
                        requestPermissions()
                    }
                }
            }
            updateMLStatus("🤖 ML Systems: Distress Detector ✓")
            
            // Initialize Hotword Detector
            hotwordDetector = HotwordDetector(this) {
                runOnUiThread {
                    Toast.makeText(this, "Help command detected! Triggering emergency response...", Toast.LENGTH_LONG).show()
                    if (checkPermissions()) {
                        fetchAndSendLocation()
                    } else {
                        requestPermissions()
                    }
                }
            }
            hotwordDetector.start()
            updateMLStatus("🤖 ML Systems: All Active ✓")
            
        } catch (e: Exception) {
            updateMLStatus("🤖 ML Systems: Error - ${e.message}")
        }
    }
    
    private fun updateMLStatus(status: String) {
        runOnUiThread {
            mlStatusText.text = status
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup ML components
        distressDetector.stop()
        hotwordDetector.stop()
        EmotionAnalyzer.cleanup()
    }
}
