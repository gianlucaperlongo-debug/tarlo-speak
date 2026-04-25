package com.tarlo.speak.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.tarlo.speak.model.AudioDocument
import com.tarlo.speak.model.TtsVoiceInfo
import java.io.File
import java.util.Locale

class TtsManager(
    context: Context,
    private val preferredVoiceName: String?,
    private val onReadyChanged: (Boolean, List<TtsVoiceInfo>) -> Unit,
    private val onChunkStarted: () -> Unit,
    private val onChunkDone: () -> Unit,
    private val onRangeStart: (Int, Int) -> Unit,
    private val onError: () -> Unit
) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var selectedVoiceName: String? = preferredVoiceName

    val availableVoices: List<TtsVoiceInfo>
        get() = getDeviceVoices()
            .orEmpty()
            .filter { it.locale.language == Locale.ITALIAN.language || it.locale.language == Locale.ENGLISH.language }
            .map { voice ->
                TtsVoiceInfo(
                    name = voice.name,
                    languageTag = voice.locale.toLanguageTag(),
                    displayLanguage = voice.locale.getDisplayName(Locale.getDefault()).ifBlank { voice.locale.toLanguageTag() },
                    quality = qualityLabel(voice.quality),
                    isItalian = voice.locale.language == Locale.ITALIAN.language,
                    isEnglish = voice.locale.language == Locale.ENGLISH.language
                )
            }
            .sortedWith(
                compareByDescending<TtsVoiceInfo> { it.isItalian }
                    .thenByDescending { it.isEnglish }
                    .thenBy { it.displayLanguage }
                    .thenBy { it.name }
            )

    override fun onInit(status: Int) {
        val ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale.ITALIAN
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = onChunkStarted()
                override fun onDone(utteranceId: String?) = onChunkDone()

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    onRangeStart(start, end)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = onError()
            })
            val selectedVoice = getDeviceVoices()
                .firstOrNull { it.name == preferredVoiceName }
                ?: getDeviceVoices().firstOrNull { it.locale.language == Locale.ITALIAN.language }
                ?: getDeviceVoices().firstOrNull { it.locale.language == Locale.ENGLISH.language }
            selectedVoice?.let {
                selectedVoiceName = it.name
                tts?.voice = it
            }
        }
        onReadyChanged(ready, availableVoices)
    }

    fun speak(document: AudioDocument) {
        val chunk = document.currentChunk
        if (chunk.isBlank()) return

        applySelectedVoice()
        tts?.setSpeechRate(document.speed)
        tts?.setPitch(document.pitch)
        tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, "chunk-${document.id}-${document.index}")
    }

    fun stop() {
        tts?.stop()
    }

    fun setVoiceByName(name: String) {
        selectedVoiceName = name
        val voice = getDeviceVoices().firstOrNull { it.name == name } ?: return
        tts?.voice = voice
    }

    fun testVoice(speed: Float, pitch: Float) {
        applySelectedVoice()
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)
        tts?.speak(
            "Ciao, questa e la voce selezionata per Tarlo Speak.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "voice-test"
        )
    }

    fun synthesizeToFile(text: String, file: File, utteranceId: String) {
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        tts?.synthesizeToFile(text, params, file, utteranceId)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun qualityLabel(quality: Int): String {
        return when {
            quality >= 500 -> "Qualita molto alta"
            quality >= 400 -> "Qualita alta"
            quality >= 300 -> "Qualita normale"
            quality >= 200 -> "Qualita bassa"
            else -> "Qualita molto bassa"
        }
    }

    private fun getDeviceVoices() = tts?.getVoices().orEmpty()

    private fun applySelectedVoice() {
        selectedVoiceName?.let { setVoiceByName(it) }
    }
}
