package com.tarlo.speak.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.tarlo.speak.model.TtsVoiceInfo
import java.io.File
import java.util.Locale

class AndroidTtsProvider(
    context: Context,
    private val preferredVoiceName: String?,
    private val onReadyChanged: (Boolean, List<TtsVoiceInfo>) -> Unit,
    private val onChunkStarted: () -> Unit,
    private val onChunkDone: () -> Unit,
    private val onRangeStart: (Int, Int) -> Unit,
    private val onError: () -> Unit
) : TextToSpeech.OnInitListener, TtsProvider {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var selectedVoiceName: String? = preferredVoiceName

    val availableVoices: List<TtsVoiceInfo>
        get() = getDeviceVoices()
            .map { voice ->
                TtsVoiceInfo(
                    name = voice.name,
                    languageTag = voice.locale.toLanguageTag(),
                    displayLanguage = voice.locale.getDisplayName(Locale.getDefault()).ifBlank { voice.locale.toLanguageTag() },
                    quality = qualityLabel(voice.quality),
                    latency = latencyLabel(voice.latency),
                    requiresNetwork = voice.isNetworkConnectionRequired,
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

    override fun speak(text: String, speed: Float, pitch: Float, utteranceId: String) {
        if (text.isBlank()) return
        applySelectedVoice()
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun stop() {
        tts?.stop()
    }

    fun setVoiceByName(name: String) {
        selectedVoiceName = name
        val voice = getDeviceVoices().firstOrNull { it.name == name } ?: return
        tts?.voice = voice
    }

    fun synthesizeToFile(text: String, file: File, utteranceId: String) {
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        tts?.synthesizeToFile(text, params, file, utteranceId)
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun qualityLabel(quality: Int): String {
        return when {
            quality >= Voice.QUALITY_VERY_HIGH -> "Qualita molto alta"
            quality >= Voice.QUALITY_HIGH -> "Qualita alta"
            quality >= Voice.QUALITY_NORMAL -> "Qualita normale"
            quality >= Voice.QUALITY_LOW -> "Qualita bassa"
            else -> "Qualita molto bassa"
        }
    }

    private fun latencyLabel(latency: Int): String {
        return when {
            latency <= Voice.LATENCY_VERY_LOW -> "Latenza molto bassa"
            latency <= Voice.LATENCY_LOW -> "Latenza bassa"
            latency <= Voice.LATENCY_NORMAL -> "Latenza normale"
            latency <= Voice.LATENCY_HIGH -> "Latenza alta"
            else -> "Latenza molto alta"
        }
    }

    private fun getDeviceVoices() = tts?.voices.orEmpty()

    private fun applySelectedVoice() {
        selectedVoiceName?.let { setVoiceByName(it) }
    }
}
