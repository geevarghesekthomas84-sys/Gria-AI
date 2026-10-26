package com.nexusai.app.core.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * 100% Offline Text-To-Speech engine using Android's native offline synthesizer.
 * Provides speech playback, markdown sanitization for audio, and utterance tracking.
 */
class OfflineVoiceEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentSpeakingId = MutableStateFlow<String?>(null)
    val currentSpeakingId: StateFlow<String?> = _currentSpeakingId.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to US English if device local is not available
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(1.02f)
            tts?.setPitch(1.0f)
            setupListener()
            isInitialized = true
            Log.i("OfflineVoiceEngine", "TextToSpeech successfully initialized offline")
        } else {
            Log.e("OfflineVoiceEngine", "Failed to initialize TextToSpeech (status: $status)")
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                _currentSpeakingId.value = utteranceId
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _currentSpeakingId.value = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _currentSpeakingId.value = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                _currentSpeakingId.value = null
                Log.w("OfflineVoiceEngine", "TTS error code $errorCode for utterance $utteranceId")
            }
        })
    }

    /**
     * Clean markdown syntax into human-friendly spoken text
     * (omits raw code syntax, hashes, bullets, bold asterisks).
     */
    fun cleanMarkdownForSpeech(rawText: String): String {
        return rawText
            // Replace fenced code blocks with a brief spoken note
            .replace(Regex("```[\\w]*\\n[\\s\\S]*?```"), " Code block provided in chat. ")
            // Inline code `foo` -> foo
            .replace(Regex("`([^`]+)`"), "$1")
            // Headers # Heading -> Heading
            .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
            // Bold/italic **text** / *text* -> text
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("__([^_]+)__"), "$1")
            .replace(Regex("_([^_]+)_"), "$1")
            // Links [title](url) -> title
            .replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")
            // Bullet points
            .replace(Regex("^[\\s]*[-*+]\\s+", RegexOption.MULTILINE), "")
            // Numbered lists 1. -> ""
            .replace(Regex("^[\\s]*\\d+\\.\\s+", RegexOption.MULTILINE), "")
            // Remove emojis or consecutive special symbols
            .replace(Regex("[>|~`#]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Speaks the given text aloud.
     * @param utteranceId unique identifier (e.g. messageId)
     * @param rawText text to speak (will be sanitized of markdown)
     */
    fun speak(utteranceId: String, rawText: String) {
        if (!isInitialized || tts == null) {
            Log.w("OfflineVoiceEngine", "TTS not initialized yet")
            return
        }

        // If already speaking this message, toggle stop
        if (_isSpeaking.value && _currentSpeakingId.value == utteranceId) {
            stop()
            return
        }

        stop()

        val textToSpeak = cleanMarkdownForSpeech(rawText)
        if (textToSpeak.isBlank()) return

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w("OfflineVoiceEngine", "Error stopping TTS", e)
        }
        _isSpeaking.value = false
        _currentSpeakingId.value = null
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w("OfflineVoiceEngine", "Error shutting down TTS", e)
        }
        tts = null
        isInitialized = false
    }
}
