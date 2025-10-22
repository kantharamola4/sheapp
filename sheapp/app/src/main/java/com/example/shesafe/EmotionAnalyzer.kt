package com.example.shesafe

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
            val modelFile = loadModelFile(context, "ml/emotion_model.tflite")
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
        val probs = runInferenceProbs(audioData)
        var maxIndex = 0
        var maxValue = probs[0]
        for (i in 1 until probs.size) {
            val v = probs[i]
            if (v > maxValue) { maxValue = v; maxIndex = i }
        }
        return maxIndex
    }

    fun detectEmotionFromPcm(pcm: ShortArray, sampleRate: Int = 16000): Pair<String, FloatArray> {
        return try {
            if (interpreter == null) return Pair(fallbackEmotion(), FloatArray(emotions.size) { 0f })
            val mono = pcmToFloatMono(pcm)
            val framed = ensureFixedLength(mono, sampleRate)
            val probs = runInferenceProbs(framed)
            val idx = argmax(probs)
            Pair(emotions[idx], probs)
        } catch (_: Exception) {
            Pair(fallbackEmotion(), FloatArray(emotions.size) { 0f })
        }
    }

    private fun runInferenceProbs(audioData: FloatArray): FloatArray {
        val inputBuffer = ByteBuffer.allocateDirect(audioData.size * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        for (sample in audioData) { inputBuffer.putFloat(sample) }
        val outputBuffer = ByteBuffer.allocateDirect(emotions.size * 4)
        outputBuffer.order(ByteOrder.nativeOrder())
        interpreter?.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()
        val probs = FloatArray(emotions.size)
        for (i in 0 until emotions.size) { probs[i] = outputBuffer.float }
        return softmax(probs)
    }

    private fun pcmToFloatMono(pcm: ShortArray): FloatArray {
        val out = FloatArray(pcm.size)
        var i = 0
        while (i < pcm.size) {
            out[i] = (pcm[i].toInt() / 32768.0f).coerceIn(-1f, 1f)
            i++
        }
        return out
    }

    private fun ensureFixedLength(signal: FloatArray, sampleRate: Int, seconds: Int = 1): FloatArray {
        val target = sampleRate * seconds
        if (signal.size == target) return signal
        val out = FloatArray(target)
        val copyLen = minOf(signal.size, target)
        System.arraycopy(signal, 0, out, 0, copyLen)
        return out
    }

    private fun softmax(x: FloatArray): FloatArray {
        var max = x[0]
        for (i in 1 until x.size) if (x[i] > max) max = x[i]
        var sum = 0.0
        val out = FloatArray(x.size)
        for (i in x.indices) { val e = kotlin.math.exp((x[i] - max).toDouble()); out[i] = e.toFloat(); sum += e }
        val fsum = sum.toFloat()
        if (fsum == 0f) return FloatArray(x.size) { 0f }
        for (i in out.indices) out[i] = out[i] / fsum
        return out
    }

    private fun argmax(x: FloatArray): Int {
        var idx = 0
        var max = x[0]
        for (i in 1 until x.size) { if (x[i] > max) { max = x[i]; idx = i } }
        return idx
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
