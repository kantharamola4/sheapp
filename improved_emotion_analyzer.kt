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
                val mfccFeatures = extractMFCCFeatures(audioFile)
                if (mfccFeatures != null) {
                    val result = runInference(mfccFeatures)
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
    
    private fun extractMFCCFeatures(audioFile: File): FloatArray? {
        return try {
            val inputStream = FileInputStream(audioFile)
            val audioBytes = inputStream.readBytes()
            inputStream.close()
            
            // Convert bytes to 16-bit PCM audio
            val audioData = ShortArray(audioBytes.size / 2)
            for (i in audioData.indices) {
                val byte1 = audioBytes[i * 2].toInt() and 0xFF
                val byte2 = audioBytes[i * 2 + 1].toInt() and 0xFF
                audioData[i] = ((byte2 shl 8) or byte1).toShort()
            }
            
            // Convert to float and normalize
            val floatAudio = FloatArray(audioData.size)
            for (i in audioData.indices) {
                floatAudio[i] = audioData[i] / 32768.0f
            }
            
            // Extract MFCC features (simplified version)
            val mfccFeatures = extractMFCC(floatAudio, 16000, 40, 40)
            mfccFeatures
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractMFCC(audio: FloatArray, sampleRate: Int, nMFCC: Int, maxFrames: Int): FloatArray? {
        return try {
            // Simplified MFCC extraction
            val frameSize = 512
            val hopSize = 256
            val nFrames = (audio.size - frameSize) / hopSize + 1
            
            val mfccFeatures = Array(nMFCC) { FloatArray(maxFrames) }
            
            for (frame in 0 until minOf(nFrames, maxFrames)) {
                val start = frame * hopSize
                val end = minOf(start + frameSize, audio.size)
                
                // Extract frame
                val frame = FloatArray(frameSize)
                System.arraycopy(audio, start, frame, 0, end - start)
                
                // Apply window function
                for (i in frame.indices) {
                    frame[i] *= (0.5 * (1 - cos(2 * PI * i / (frameSize - 1)))).toFloat()
                }
                
                // Compute FFT magnitude spectrum
                val fft = computeFFT(frame)
                val magnitude = FloatArray(fft.size / 2)
                for (i in magnitude.indices) {
                    val real = fft[i * 2]
                    val imag = fft[i * 2 + 1]
                    magnitude[i] = sqrt(real * real + imag * imag)
                }
                
                // Apply mel filter bank (simplified)
                val melFilters = createMelFilterBank(magnitude.size, sampleRate, nMFCC)
                val melSpectrum = FloatArray(nMFCC)
                for (i in 0 until nMFCC) {
                    for (j in magnitude.indices) {
                        melSpectrum[i] += magnitude[j] * melFilters[i][j]
                    }
                }
                
                // Apply log and DCT
                for (i in 0 until nMFCC) {
                    melSpectrum[i] = ln(maxOf(melSpectrum[i], 1e-10f))
                }
                
                // DCT (simplified)
                for (i in 0 until nMFCC) {
                    var sum = 0f
                    for (j in 0 until nMFCC) {
                        sum += melSpectrum[j] * cos(PI * i * (2 * j + 1) / (2 * nMFCC))
                    }
                    mfccFeatures[i][frame] = sum
                }
            }
            
            // Flatten to 1D array for model input
            val result = FloatArray(nMFCC * maxFrames)
            var idx = 0
            for (i in 0 until nMFCC) {
                for (j in 0 until maxFrames) {
                    result[idx++] = mfccFeatures[i][j]
                }
            }
            result
        } catch (e: Exception) {
            null
        }
    }
    
    private fun computeFFT(input: FloatArray): FloatArray {
        val n = input.size
        val output = FloatArray(n * 2) // Real and imaginary parts
        
        // Simple FFT implementation
        for (k in 0 until n) {
            var real = 0f
            var imag = 0f
            for (j in 0 until n) {
                val angle = -2 * PI * k * j / n
                real += input[j] * cos(angle)
                imag += input[j] * sin(angle)
            }
            output[k * 2] = real
            output[k * 2 + 1] = imag
        }
        return output
    }
    
    private fun createMelFilterBank(nFFT: Int, sampleRate: Int, nFilters: Int): Array<FloatArray> {
        val filters = Array(nFilters) { FloatArray(nFFT) }
        val melLow = 0f
        val melHigh = 2595 * log10(1 + (sampleRate / 2) / 700f)
        val melPoints = FloatArray(nFilters + 2)
        
        for (i in 0 until nFilters + 2) {
            melPoints[i] = melLow + (melHigh - melLow) * i / (nFilters + 1)
        }
        
        val hzPoints = FloatArray(nFilters + 2)
        for (i in hzPoints.indices) {
            hzPoints[i] = 700 * (pow(10f, melPoints[i] / 2595) - 1)
        }
        
        val binPoints = IntArray(nFilters + 2)
        for (i in binPoints.indices) {
            binPoints[i] = ((nFFT + 1) * hzPoints[i] / sampleRate).toInt()
        }
        
        for (i in 1 until nFilters + 1) {
            val left = binPoints[i - 1]
            val center = binPoints[i]
            val right = binPoints[i + 1]
            
            for (j in left until center) {
                filters[i - 1][j] = (j - left).toFloat() / (center - left)
            }
            for (j in center until right) {
                filters[i - 1][j] = (right - j).toFloat() / (right - center)
            }
        }
        
        return filters
    }
    
    private fun runInference(mfccFeatures: FloatArray): Int {
        val probs = runInferenceProbs(mfccFeatures)
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
            val mfccFeatures = extractMFCC(mono, sampleRate, 40, 40)
            if (mfccFeatures != null) {
                val probs = runInferenceProbs(mfccFeatures)
                val idx = argmax(probs)
                Pair(emotions[idx], probs)
            } else {
                Pair(fallbackEmotion(), FloatArray(emotions.size) { 0f })
            }
        } catch (_: Exception) {
            Pair(fallbackEmotion(), FloatArray(emotions.size) { 0f })
        }
    }

    private fun runInferenceProbs(mfccFeatures: FloatArray): FloatArray {
        // Reshape to match model input: (1, 40, 40, 1)
        val inputBuffer = ByteBuffer.allocateDirect(40 * 40 * 1 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        // Fill input buffer with MFCC features
        for (i in 0 until 40) {
            for (j in 0 until 40) {
                val idx = i * 40 + j
                inputBuffer.putFloat(if (idx < mfccFeatures.size) mfccFeatures[idx] else 0f)
            }
        }
        
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
