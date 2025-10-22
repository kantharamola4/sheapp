package com.example.shesafe
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.example.shesafe.EmotionAnalyzer

object AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRecording = false

    fun startRecording(context: Context, sampleRate: Int = 16000) {
        if (isRecording) return
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf
        )
        try {
            audioRecord?.startRecording()
            isRecording = true
        } catch (_: Exception) {
            isRecording = false
        }
    }

    private fun stopRecordingAndReadPcm(maxMillis: Int = 1000, sampleRate: Int = 16000): ShortArray {
        val record = audioRecord ?: return ShortArray(0)
        val totalSamples = sampleRate * maxMillis / 1000
        val buffer = ShortArray(totalSamples)
        var offset = 0
        try {
            val tmp = ShortArray(2048)
            val start = System.currentTimeMillis()
            while (isRecording && offset < totalSamples && (System.currentTimeMillis() - start) < (maxMillis + 100)) {
                val n = record.read(tmp, 0, tmp.size)
                if (n > 0) {
                    val copy = minOf(n, totalSamples - offset)
                    System.arraycopy(tmp, 0, buffer, offset, copy)
                    offset += copy
                }
            }
        } catch (_: Exception) {
        } finally {
            try { record.stop() } catch (_: Exception) {}
            try { record.release() } catch (_: Exception) {}
            audioRecord = null
            isRecording = false
        }
        return if (offset == buffer.size) buffer else buffer.copyOf(offset)
    }

    fun stopRecordingAndAnalyze(context: Context, onEmotionDetected: (String) -> Unit) {
        val pcm = stopRecordingAndReadPcm(1000, 16000)
        if (pcm.isNotEmpty()) {
            val (label, probs) = EmotionAnalyzer.detectEmotionFromPcm(pcm, 16000)
            onEmotionDetected(label)
        } else {
            onEmotionDetected("unknown")
        }
    }
}