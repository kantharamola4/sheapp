package com.example.shesafe

import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val requireFace = findViewById<Switch>(R.id.switchRequireFace)
        val enableHotword = findViewById<Switch>(R.id.switchEnableHotword)

        requireFace.isChecked = prefs.getBoolean("require_face", false)
        enableHotword.isChecked = prefs.getBoolean("enable_hotword", true)

        requireFace.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("require_face", isChecked).apply()
        }
        enableHotword.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_hotword", isChecked).apply()
        }
    }
}


