package com.example.shesafe.utils

import java.io.File

object EmotionAnalyzer {

    // This is just a placeholder function
    fun detectEmotion(audioFile: File): String {
        // In future: send to ML model or API
        val possibleEmotions = listOf("happy", "sad", "angry", "fear", "neutral")

        // Fake random result for testing
        return possibleEmotions.random()
    }
}
