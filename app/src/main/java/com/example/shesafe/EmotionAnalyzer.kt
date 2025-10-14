package com.example.shesafe.utils

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.*

object EmotionAnalyzer {
    
    private var interpreter: Interpreter? = null
    private val emotions = listOf("angry", "fear", "happy", "neutral", "sad", "surprise")
    
    fun initialize(context: Context) {
        try {
            val modelFile = loadModelFile(context, "emotion_model.tflite")
            interpreter = Interpreter(modelFile)
        } catch (e: Exception) {
            // Fallback to heuristic if model fails
            interpreter = null
        }
    }
    
    fun detectEmotion(audioFile: File): String {
        return try {
            if (interpreter != null) {
                val audioData = preprocessAudio(audioFile)
                if (audioData != null) {
                    val result = runInference(audioData)
                    emotions[result]
                } else {
                    fallbackEmotion()
                }
            } else {
                fallbackEmotion()
            }
        } catch (e: Exception) {
            fallbackEmotion()
        }
    }
    
    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    private fun preprocessAudio(audioFile: File): FloatArray? {
        return try {
            // Simple audio preprocessing - extract features from raw audio
            val inputStream = FileInputStream(audioFile)
            val audioBytes = inputStream.readBytes()
            inputStream.close()
            
            // Convert to float array and normalize
            val audioData = FloatArray(minOf(audioBytes.size, 16000)) // Limit to 1 second at 16kHz
            for (i in audioData.indices) {
                audioData[i] = (audioBytes[i].toInt() and 0xFF) / 255.0f - 0.5f
            }
            
            // Pad or truncate to fixed size
            val fixedSize = 16000
            val result = FloatArray(fixedSize)
            System.arraycopy(audioData, 0, result, 0, minOf(audioData.size, fixedSize))
            result
        } catch (e: Exception) {
            null
        }
    }
    
    private fun runInference(audioData: FloatArray): Int {
        val inputBuffer = ByteBuffer.allocateDirect(audioData.size * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        for (sample in audioData) {
            inputBuffer.putFloat(sample)
        }
        
        val outputBuffer = ByteBuffer.allocateDirect(emotions.size * 4)
        outputBuffer.order(ByteOrder.nativeOrder())
        
        interpreter?.run(inputBuffer, outputBuffer)
        
        // Find emotion with highest probability
        outputBuffer.rewind()
        var maxIndex = 0
        var maxValue = outputBuffer.float
        for (i in 1 until emotions.size) {
            val value = outputBuffer.float
            if (value > maxValue) {
                maxValue = value
                maxIndex = i
            }
        }
        
        return maxIndex
    }
    
    private fun fallbackEmotion(): String {
        // Safety-biased heuristic for emergency detection
        val weighted = listOf("fear", "fear", "angry", "panic", "neutral", "sad", "happy")
        return weighted.random()
    }
    
    fun cleanup() {
        interpreter?.close()
        interpreter = null
    }
}
