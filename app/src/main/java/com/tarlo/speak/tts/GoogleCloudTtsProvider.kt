package com.tarlo.speak.tts

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
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
    private val onCharactersWillBeSent: (Int) -> Boolean,
    private val onError: (String) -> Unit
) : TtsProvider {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main)
    private var requestJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var apiKey: String = ""
    private var voiceName: String = DEFAULT_VOICE

    fun configure(apiKey: String, voiceName: String) {
        this.apiKey = apiKey.trim()
        this.voiceName = voiceName.ifBlank { DEFAULT_VOICE }
    }

    override fun speak(text: String, speed: Float, pitch: Float, utteranceId: String) {
        if (apiKey.isBlank()) {
            onError("Inserisci una API key Google Cloud")
            return
        }

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
        val allowed = runBlocking(Dispatchers.Main) { onCharactersWillBeSent(text.length) }
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
        val file = File(appContext.cacheDir, "tarlo_google_tts_$utteranceId.mp3")
        file.writeBytes(bytes)
        return file
    }

    private fun synthesizeToTempMp3WithFallbackVoice(text: String, speed: Float, pitch: Float, utteranceId: String): File {
        return runCatching {
            synthesizeToTempMp3(text, speed, pitch, utteranceId)
        }.getOrElse { firstError ->
            val message = firstError.message.orEmpty().lowercase()
            if ("voce" !in message && "richiesta" !in message) throw firstError
            val alternative = ITALIAN_WAVENET_VOICES.firstOrNull { it != voiceName } ?: DEFAULT_VOICE
            if (alternative == voiceName) throw firstError
            voiceName = alternative
            synthesizeToTempMp3(text, speed, pitch, "${utteranceId}_fallback")
        }
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
                onError("Errore durante la riproduzione della voce premium")
                true
            }
            prepare()
            start()
        }
    }

    private fun buildRequestJson(text: String, speed: Float, pitch: Float): String {
        val safeRate = speed.coerceIn(0.25f, 4.0f)
        val safePitch = ((pitch - 1.0f) * 20f).coerceIn(-20f, 20f)
        return """
            {
              "input": { "text": "${escapeJson(text)}" },
              "voice": {
                "languageCode": "it-IT",
                "name": "${escapeJson(voiceName)}"
              },
              "audioConfig": {
                "audioEncoding": "MP3",
                "speakingRate": $safeRate,
                "pitch": $safePitch
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
        val ITALIAN_WAVENET_VOICES = listOf(
            "it-IT-Wavenet-A",
            "it-IT-Wavenet-B",
            "it-IT-Wavenet-C",
            "it-IT-Wavenet-D"
        )
    }
}
