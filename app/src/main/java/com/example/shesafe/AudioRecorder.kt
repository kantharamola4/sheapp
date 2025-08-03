package com.example.shesafe

import android.content.Context
import android.media.MediaRecorder
import android.os.Handler
import java.io.File

object AudioRecorder {
    private var recorder: MediaRecorder? = null
    private val handler = Handler()

    fun startRecording(context: Context) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val file = File(context.filesDir, "audio_${System.currentTimeMillis()}.3gp")
                recorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }

                handler.postDelayed({
                    recorder?.stop()
                    recorder?.release()
                    recorder = null
                    handler.postDelayed(this, 5000)
                }, 5000)
            }
        }, 1000)
    }
}
