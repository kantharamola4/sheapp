package com.example.shesafe
import android.content.Context
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import java.io.File
import com.example.shesafe.utils.EmotionAnalyzer

object AudioRecorder {
    private var recorder: MediaRecorder? = null
    private val handler = Handler(Looper.getMainLooper())
    private var outputFilePath: String? = null

    fun startRecording(context: Context) {
        val file = File(context.filesDir, "audio_${System.currentTimeMillis()}.3gp")
        outputFilePath = file.absolutePath

        try {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFilePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            recorder = null
        }
    }

    private fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder = null
        }
    }

    // ...
    fun stopRecordingAndAnalyze(context: Context, onEmotionDetected: (String) -> Unit) {
        stopRecording()
        val file = outputFilePath?.let { File(it) }
        if (file != null && file.exists()) {
            val emotion = EmotionAnalyzer.detectEmotion(file) // <-- ERROR HERE
            onEmotionDetected(emotion)
        } else {
            onEmotionDetected("unknown")
        }
    }
// ...
}