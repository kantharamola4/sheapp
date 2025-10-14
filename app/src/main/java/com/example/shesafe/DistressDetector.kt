package com.example.shesafe

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.abs

class DistressDetector {
    private var recorder: AudioRecord? = null
    private var thread: Thread? = null
    @Volatile private var running = false
    private var baseline = 0.0

    fun start(onDistress: () -> Unit) {
        if (running) return
        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf)
        running = true
        recorder?.startRecording()
        thread = Thread {
            val buffer = ShortArray(minBuf / 2)
            var frames = 0
            while (running) {
                val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val avg = buffer.take(read).map { abs(it.toInt()) }.average()
                    if (frames < 50) {
                        baseline = if (frames == 0) avg else (baseline * 0.9 + avg * 0.1)
                        frames++
                    } else {
                        val ratio = if (baseline <= 1.0) 1.0 else avg / baseline
                        if (ratio > 4.0 || avg > 10_000) {
                            onDistress()
                            try { Thread.sleep(3000) } catch (_: InterruptedException) {}
                        }
                    }
                }
            }
        }
        thread?.start()
    }

    fun stop() {
        running = false
        try { thread?.join(500) } catch (_: Exception) {}
        recorder?.stop()
        recorder?.release()
        recorder = null
        thread = null
    }
}


