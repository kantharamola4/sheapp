package com.example.shesafe

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextProvider.init(this)
        // Initialize emotion analyzer with TFLite model
        try {
            com.example.shesafe.utils.EmotionAnalyzer.initialize(this)
        } catch (e: Exception) {
            // Continue with fallback heuristic
        }
    }
}

object AppContextProvider {
    lateinit var context: android.content.Context
        private set

    fun init(app: Application) {
        context = app.applicationContext
    }
}


