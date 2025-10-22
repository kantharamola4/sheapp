package com.example.shesafe

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val nameEdit = findViewById<EditText>(R.id.nameEdit)
        val phoneEdit = findViewById<EditText>(R.id.phoneEdit)
        val ageEdit = findViewById<EditText>(R.id.ageEdit)
        val bloodEdit = findViewById<EditText>(R.id.bloodEdit)
        val saveBtn = findViewById<Button>(R.id.saveProfileBtn)

        val prefs = getSharedPreferences("user_profile", MODE_PRIVATE)
        nameEdit.setText(prefs.getString("name", ""))
        phoneEdit.setText(prefs.getString("phone", ""))
        ageEdit.setText(prefs.getString("age", ""))
        bloodEdit.setText(prefs.getString("blood", ""))

        saveBtn.setOnClickListener {
            prefs.edit().apply {
                putString("name", nameEdit.text.toString())
                putString("phone", phoneEdit.text.toString())
                putString("age", ageEdit.text.toString())
                putString("blood", bloodEdit.text.toString())
                apply()
            }
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }
    }
}
