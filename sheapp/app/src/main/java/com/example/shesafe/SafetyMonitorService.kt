package com.example.shesafe

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class SafetyMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false
    private var hotword: HotwordDetector? = null
    private var distress: DistressDetector? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isMonitoring) {
            isMonitoring = true
            scheduleNextSweep()
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val enableHotword = prefs.getBoolean("enable_hotword", true)
            val hasMic = hasPermission(Manifest.permission.RECORD_AUDIO)
            if (enableHotword && hasMic) {
                hotword = HotwordDetector(this) { triggerSos() }.also { it.start() }
            }
            if (hasMic) {
                distress = DistressDetector().also { d -> d.start { triggerSos() } }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        handler.removeCallbacksAndMessages(null)
        hotword?.stop(); hotword = null
        distress?.stop(); distress = null
        // Cleanup emotion analyzer
        try {
            EmotionAnalyzer.cleanup()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scheduleNextSweep() {
        if (!isMonitoring) return
        handler.postDelayed({ performEmotionSweep() }, 5_000)
    }

    private fun performEmotionSweep() {
        // Record a short snippet and analyze. If distressed, trigger SOS.
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            scheduleNextSweep()
            return
        }
        
        try {
            AudioRecorder.startRecording(this)
            handler.postDelayed({
                AudioRecorder.stopRecordingAndAnalyze(this) { emotion ->
                    // Enhanced emotion detection for emergency situations
                    val distressEmotions = listOf("fear", "panic", "angry", "surprise")
                    if (distressEmotions.any { emotion.equals(it, ignoreCase = true) }) {
                        triggerSos()
                    } else {
                        scheduleNextSweep()
                    }
                }
            }, 2_000)
        } catch (e: Exception) {
            // Log error and continue
            android.util.Log.e("SafetyMonitor", "Emotion sweep failed: ${e.message}")
            scheduleNextSweep()
        }
    }

    private fun triggerSos() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val requireFace = prefs.getBoolean("require_face", false)

        if (requireFace) {
            // One-shot quick face presence gate (1–2s timeout)
            val gateHandler = Handler(Looper.getMainLooper())
            var decided = false
            // Capture one frame with face detection
            try { 
                CameraCapture.startCapturing(this) { faceDetected ->
                    if (!decided) {
                        decided = true
                        if (faceDetected) {
                            proceedSos()
                        } else {
                            // No face detected, continue monitoring
                            scheduleNextSweep()
                        }
                    }
                }
            } catch (_: Exception) {
                proceedSos()
            }
            // Fallback timeout
            gateHandler.postDelayed({
                if (!decided) {
                    decided = true
                    proceedSos()
                }
            }, 2000)
        } else {
            proceedSos()
        }
    }

    private fun proceedSos() {
        // 1) Capture photos quietly with face detection
        if (hasPermission(Manifest.permission.CAMERA)) {
            try { 
                CameraCapture.startCapturing(this) { faceDetected ->
                    android.util.Log.d("SafetyMonitor", "Face detected during SOS: $faceDetected")
                }
            } catch (_: Exception) {}
        }

        // 2) Record short audio for evidence
        if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
            try {
                AudioRecorder.startRecording(this)
                handler.postDelayed({
                    try { AudioRecorder.stopRecordingAndAnalyze(this) { _ -> } } catch (_: Exception) {}
                }, 4_000)
            } catch (_: Exception) {}
        }

        // 3) Get location and upload metadata
        LocationHelper.getCurrentLocation(this) { location ->
            val lat = location?.latitude
            val lon = location?.longitude
            EmergencyUploader.enqueueEmergencyUpload(this, lat, lon)
        }

        // 4) Also send legacy SMS as fallback
        val intent = Intent(this, PanicService::class.java)
        startService(intent)

        // Continue monitoring after a cooldown
        handler.postDelayed({ scheduleNextSweep() }, 15_000)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun startInForeground() {
        val channelId = "safety_monitor"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Safety Monitor", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Safety monitoring active")
            .setContentText("Listening for distress and hotword")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
        startForeground(1001, notification)
    }
}


