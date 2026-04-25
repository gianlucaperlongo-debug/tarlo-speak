package com.tarlo.speak.tts

import android.content.Context
import com.tarlo.speak.model.TtsProviderType
import com.tarlo.speak.model.TtsVoiceInfo
import java.io.File

class TtsManager(
    context: Context,
    preferredVoiceName: String?,
    private val onReadyChanged: (Boolean, List<TtsVoiceInfo>) -> Unit,
    private val onChunkStarted: () -> Unit,
    private val onChunkDone: () -> Unit,
    private val onRangeStart: (Int, Int) -> Unit,
    private val onGoogleCharactersWillBeSent: (TtsProviderType, Int) -> Boolean,
    private val onGoogleCharactersSent: (TtsProviderType, Int) -> Unit,
    private val onCacheHit: (Int) -> Unit,
    private val onCacheStored: () -> Unit,
    private val onCacheWriteSkipped: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val androidProvider = AndroidTtsProvider(
        context = context,
        preferredVoiceName = preferredVoiceName,
        onReadyChanged = onReadyChanged,
        onChunkStarted = onChunkStarted,
        onChunkDone = onChunkDone,
        onRangeStart = onRangeStart,
        onError = { onError("Errore durante la lettura") }
    )

    private val googleProvider = GoogleCloudTtsProvider(
        context = context,
        onChunkStarted = onChunkStarted,
        onChunkDone = onChunkDone,
        onCharactersCanBeSent = onGoogleCharactersWillBeSent,
        onCharactersSent = onGoogleCharactersSent,
        onCacheHit = onCacheHit,
        onCacheStored = onCacheStored,
        onCacheWriteSkipped = onCacheWriteSkipped,
        onError = onError
    )

    private var providerType: TtsProviderType = TtsProviderType.ANDROID

    val availableVoices: List<TtsVoiceInfo>
        get() = androidProvider.availableVoices

    fun configureProvider(type: TtsProviderType, googleApiKey: String, googleVoiceName: String) {
        providerType = type
        googleProvider.configure(googleApiKey, googleVoiceName, type)
    }

    fun configureCache(enabled: Boolean, onlyCacheMode: Boolean, maxSizeMb: Int, presetName: String) {
        googleProvider.configureCache(enabled, onlyCacheMode, maxSizeMb, presetName)
    }

    fun speakText(text: String, speed: Float, pitch: Float, utteranceId: String, preferredProvider: TtsProviderType = providerType) {
        if (text.isBlank()) return
        if (preferredProvider.isGoogle) {
            androidProvider.stop()
            googleProvider.speak(text, speed, pitch, utteranceId)
        } else {
            googleProvider.stop()
            androidProvider.speak(text, speed, pitch, utteranceId)
        }
    }

    fun stop() {
        androidProvider.stop()
        googleProvider.stop()
    }

    fun setVoiceByName(name: String) {
        androidProvider.setVoiceByName(name)
    }

    fun testAndroidVoice(speed: Float, pitch: Float) {
        speakText(
            text = "Ciao, questa e la voce selezionata per Tarlo Speak.",
            speed = speed,
            pitch = pitch,
            utteranceId = "android-voice-test",
            preferredProvider = TtsProviderType.ANDROID
        )
    }

    fun testGoogleVoice(speed: Float, pitch: Float) {
        speakText(
            text = "Ciao, questa e la voce premium Google Cloud selezionata.",
            speed = speed,
            pitch = pitch,
            utteranceId = "google-voice-test",
            preferredProvider = providerType
        )
    }

    fun synthesizeToFile(text: String, file: File, utteranceId: String) {
        androidProvider.synthesizeToFile(text, file, utteranceId)
    }

    fun shutdown() {
        androidProvider.shutdown()
        googleProvider.shutdown()
    }
}
