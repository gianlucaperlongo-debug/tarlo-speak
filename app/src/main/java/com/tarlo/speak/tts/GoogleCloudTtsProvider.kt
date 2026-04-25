package com.tarlo.speak.tts

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.util.Base64
import com.tarlo.speak.model.TtsProviderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GoogleCloudTtsProvider(
    context: Context,
    private val onChunkStarted: () -> Unit,
    private val onChunkDone: () -> Unit,
    private val onCharactersCanBeSent: (TtsProviderType, Int) -> Boolean,
    private val onCharactersSent: (TtsProviderType, Int) -> Unit,
    private val onCacheHit: (Int) -> Unit,
    private val onCacheStored: () -> Unit,
    private val onCacheWriteSkipped: () -> Unit,
    private val onError: (String) -> Unit
) : TtsProvider {
    private val appContext = context.applicationContext
    private val audioCache = AudioCacheManager(appContext)
    private val scope = CoroutineScope(Dispatchers.Main)
    private var requestJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var apiKey: String = ""
    private var voiceName: String = DEFAULT_VOICE
    private var providerType: TtsProviderType = TtsProviderType.GOOGLE_WAVENET
    private var playbackSpeed: Float = 1.0f
    private var cacheEnabled: Boolean = true
    private var onlyCacheMode: Boolean = false
    private var cacheMaxSizeMb: Int = 500
    private var presetName: String = "AUDIOBOOK_PRO"

    fun configure(apiKey: String, voiceName: String, providerType: TtsProviderType) {
        this.apiKey = apiKey.trim()
        this.voiceName = voiceName.ifBlank { DEFAULT_VOICE }
        this.providerType = providerType
    }

    fun configureCache(enabled: Boolean, onlyCacheMode: Boolean, maxSizeMb: Int, presetName: String) {
        this.cacheEnabled = enabled
        this.onlyCacheMode = onlyCacheMode
        this.cacheMaxSizeMb = maxSizeMb
        this.presetName = presetName
        if (enabled) audioCache.trimToLimit(maxSizeMb)
    }

    override fun speak(text: String, speed: Float, pitch: Float, utteranceId: String) {
        playbackSpeed = speed.coerceIn(0.70f, 1.40f)
        stop()
        onChunkStarted()
        requestJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    synthesizeToTempMp3WithFallbackVoice(text, speed, pitch, utteranceId)
                }
            }

            result
                .onSuccess { file -> playFile(file) }
                .onFailure { error -> onError(error.message ?: "Google Cloud TTS non disponibile") }
        }
    }

    override fun stop() {
        requestJob?.cancel()
        requestJob = null
        mediaPlayer?.setOnCompletionListener(null)
        runCatching { mediaPlayer?.stop() }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun shutdown() {
        stop()
    }

    private fun synthesizeToTempMp3(text: String, speed: Float, pitch: Float, utteranceId: String): File {
        val cacheFile = audioCache.cacheFile(
            provider = providerType,
            voiceName = voiceName,
            language = "it-IT",
            text = text,
            speed = speed,
            pitch = pitch,
            presetName = presetName,
            chirpUsesPlayerSpeed = providerType == TtsProviderType.GOOGLE_CHIRP
        )
        if (cacheEnabled && cacheFile.exists() && cacheFile.length() > 0L) {
            cacheFile.setLastModified(System.currentTimeMillis())
            runBlocking(Dispatchers.Main) { onCacheHit(text.length) }
            return cacheFile
        }

        if (onlyCacheMode) {
            throw IllegalStateException("Modalita solo cache attiva: nessun nuovo carattere Google consumato.")
        }

        if (apiKey.isBlank()) {
            throw IllegalStateException("Inserisci una API key Google Cloud")
        }

        val allowed = runBlocking(Dispatchers.Main) { onCharactersCanBeSent(providerType, text.length) }
        if (!allowed) {
            throw IllegalStateException("Limite sicurezza Google raggiunto")
        }

        val endpoint = "https://texttospeech.googleapis.com/v1/text:synthesize?key=${URLEncoder.encode(apiKey, "UTF-8")}"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        val body = buildRequestJson(text, speed, pitch)
        connection.outputStream.use { output ->
            output.write(body.toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode
        val response = if (code in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            val details = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IllegalStateException(readableGoogleError(code, details))
        }

        val audioContent = Regex("\"audioContent\"\\s*:\\s*\"([^\"]+)\"").find(response)
            ?.groupValues
            ?.getOrNull(1)
            ?: throw IllegalStateException("Risposta Google Cloud senza audio")

        val bytes = runCatching { Base64.decode(audioContent, Base64.DEFAULT) }
            .getOrElse { throw IllegalStateException("Risposta audio Google Cloud non valida") }
        runBlocking(Dispatchers.Main) { onCharactersSent(providerType, text.length) }
        val tempFile = File(appContext.cacheDir, "tarlo_google_tts_$utteranceId.mp3")
        tempFile.writeBytes(bytes)

        if (cacheEnabled) {
            runCatching {
                audioCache.trimToLimit(cacheMaxSizeMb)
                if (audioCache.hasEnoughRoomFor(bytes.size, cacheMaxSizeMb)) {
                    cacheFile.parentFile?.mkdirs()
                    tempFile.copyTo(cacheFile, overwrite = true)
                    cacheFile.setLastModified(System.currentTimeMillis())
                    audioCache.trimToLimit(cacheMaxSizeMb)
                    runBlocking(Dispatchers.Main) { onCacheStored() }
                    return cacheFile
                } else {
                    runBlocking(Dispatchers.Main) { onCacheWriteSkipped() }
                }
            }.onFailure {
                runBlocking(Dispatchers.Main) { onCacheWriteSkipped() }
            }
        }

        return tempFile
    }

    private fun synthesizeToTempMp3WithFallbackVoice(text: String, speed: Float, pitch: Float, utteranceId: String): File {
        return synthesizeToTempMp3(text, speed, pitch, utteranceId)
    }

    private fun playFile(file: File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                it.release()
                if (mediaPlayer === it) mediaPlayer = null
                onChunkDone()
            }
            setOnErrorListener { player, _, _ ->
                player.release()
                if (mediaPlayer === player) mediaPlayer = null
                if (file.parentFile?.name == "tarlo_audio_cache") runCatching { file.delete() }
                onError("Errore durante la riproduzione della voce premium")
                true
            }
            prepare()
            if (providerType == TtsProviderType.GOOGLE_CHIRP) {
                runCatching {
                    playbackParams = PlaybackParams().setSpeed(playbackSpeed).setPitch(1.0f)
                }
            }
            start()
        }
    }

    private fun buildRequestJson(text: String, speed: Float, pitch: Float): String {
        val chirp = providerType == TtsProviderType.GOOGLE_CHIRP
        val safeRate = speed.coerceIn(0.25f, 4.0f)
        val safePitch = ((pitch - 1.0f) * 20f).coerceIn(-20f, 20f)
        val tuning = if (chirp) "" else """,
                "speakingRate": $safeRate,
                "pitch": $safePitch"""
        return """
            {
              "input": { "text": "${escapeJson(text)}" },
              "voice": {
                "languageCode": "it-IT",
                "name": "${escapeJson(voiceName)}"
              },
              "audioConfig": {
                "audioEncoding": "MP3"$tuning
              }
            }
        """.trimIndent()
    }

    private fun escapeJson(value: String): String {
        return buildString {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }

    private fun readableGoogleError(code: Int, details: String): String {
        val lower = details.lowercase()
        return when {
            code == 400 -> "Richiesta Google TTS non valida. Controlla voce e testo."
            code == 401 || code == 403 -> {
                if ("quota" in lower || "billing" in lower) {
                    "Google TTS bloccato da quota o billing. Controlla Google Cloud Console."
                } else {
                    "API key Google TTS non valida o non autorizzata."
                }
            }
            code == 429 -> "Quota Google TTS raggiunta o troppe richieste. Controlla Google Cloud Console."
            code >= 500 -> "Google TTS temporaneamente non disponibile."
            else -> "Errore Google Cloud TTS ($code)."
        }
    }

    companion object {
        const val DEFAULT_VOICE = "it-IT-Wavenet-A"
        const val DEFAULT_CHIRP_VOICE = "it-IT-Chirp3-HD-Achernar"
        val ITALIAN_WAVENET_VOICES = listOf(
            "it-IT-Wavenet-A",
            "it-IT-Wavenet-B",
            "it-IT-Wavenet-C",
            "it-IT-Wavenet-D"
        )
    }
}
