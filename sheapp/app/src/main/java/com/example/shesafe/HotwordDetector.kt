package com.example.shesafe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class HotwordDetector(private val context: Context, private val onHotword: () -> Unit) : RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        if (running) return
        running = true
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply { setRecognitionListener(this@HotwordDetector) }
        scheduleListen()
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
    }

    private fun scheduleListen() {
        if (!running) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        try { recognizer?.startListening(intent) } catch (_: Exception) {}
        // Restart listening window every 6s to keep it active
        handler.postDelayed({ scheduleListen() }, 6_000)
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onError(error: Int) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onResults(results: Bundle?) { handleResults(results) }
    override fun onPartialResults(partialResults: Bundle?) { handleResults(partialResults) }

    private fun handleResults(bundle: Bundle?) {
        val texts = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (t in texts) {
            val s = t.lowercase(Locale.getDefault())
            if (s.contains("help me") || s.contains("help!")) {
                onHotword()
                break
            }
        }
    }
}


