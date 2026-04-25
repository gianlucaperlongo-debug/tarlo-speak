package com.tarlo.speak

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.tarlo.speak.data.DocumentRepository
import com.tarlo.speak.model.AudioDocument
import com.tarlo.speak.model.TextHighlight
import com.tarlo.speak.model.TtsVoiceInfo
import com.tarlo.speak.tts.TtsManager
import com.tarlo.speak.ui.TarloSpeakApp
import com.tarlo.speak.ui.TarloUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var repository: DocumentRepository
    private lateinit var ttsManager: TtsManager
    private lateinit var preferences: SharedPreferences
    private val scope = CoroutineScope(Dispatchers.Main)

    private var documents = androidx.compose.runtime.mutableStateOf<List<AudioDocument>>(emptyList())
    private var current = androidx.compose.runtime.mutableStateOf<AudioDocument?>(null)
    private var isPlaying = androidx.compose.runtime.mutableStateOf(false)
    private var statusText = androidx.compose.runtime.mutableStateOf("Importa un EPUB per iniziare")
    private var ttsReady = androidx.compose.runtime.mutableStateOf(false)
    private var voices = androidx.compose.runtime.mutableStateOf<List<TtsVoiceInfo>>(emptyList())
    private var selectedVoiceName = androidx.compose.runtime.mutableStateOf<String?>(null)
    private var speechRate = androidx.compose.runtime.mutableStateOf(1.0f)
    private var voicePitch = androidx.compose.runtime.mutableStateOf(0.92f)
    private var textHighlight = androidx.compose.runtime.mutableStateOf(TextHighlight())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = DocumentRepository(this)
        preferences = getSharedPreferences("tarlo_voice_settings", MODE_PRIVATE)
        selectedVoiceName.value = preferences.getString(KEY_SELECTED_VOICE, null)
        speechRate.value = preferences.getFloat(KEY_SPEECH_RATE, 1.0f)
        voicePitch.value = preferences.getFloat(KEY_VOICE_PITCH, 0.92f)

        ttsManager = TtsManager(
            context = this,
            preferredVoiceName = selectedVoiceName.value,
            onReadyChanged = { ready, availableVoices ->
                runOnUiThread {
                    ttsReady.value = ready
                    voices.value = availableVoices
                    val savedVoice = selectedVoiceName.value
                    val selected = availableVoices.firstOrNull { it.name == savedVoice }
                        ?: availableVoices.firstOrNull { it.isItalian }
                        ?: availableVoices.firstOrNull()
                    selectedVoiceName.value = selected?.name
                    selected?.name?.let { saveSelectedVoice(it) }
                    statusText.value = if (ready) "Voce italiana pronta" else "Text-to-Speech non disponibile"
                }
            },
            onChunkStarted = {
                runOnUiThread {
                    textHighlight.value = fallbackHighlightForCurrentSentence()
                }
            },
            onChunkDone = {
                runOnUiThread {
                    if (isPlaying.value) moveToNextChunk()
                }
            },
            onRangeStart = { start, end ->
                runOnUiThread {
                    val textLength = current.value?.currentChunk?.length ?: 0
                    val safeStart = start.coerceIn(0, textLength)
                    val safeEnd = end.coerceIn(safeStart, textLength)
                    if (safeEnd > safeStart) {
                        textHighlight.value = TextHighlight(safeStart, safeEnd, exact = true)
                    }
                }
            },
            onError = {
                runOnUiThread {
                    isPlaying.value = false
                    statusText.value = "Errore durante la lettura"
                }
            }
        )

        setContent {
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { importEpub(it) }
                }
            }

            TarloSpeakApp(
                state = TarloUiState(
                    documents = documents.value,
                    current = current.value,
                    isPlaying = isPlaying.value,
                    statusText = statusText.value,
                    ttsReady = ttsReady.value,
                    voices = voices.value,
                    selectedVoiceName = selectedVoiceName.value,
                    speechRate = speechRate.value,
                    voicePitch = voicePitch.value,
                    highlight = textHighlight.value
                ),
                onImportClick = {
                    statusText.value = "Seleziona un file EPUB"
                    picker.launch(createOpenEpubIntent())
                },
                onSelectDocument = { selectDocument(it) },
                onTogglePlay = { togglePlay() },
                onStop = { stopReading() },
                onSkip = { skip(it) },
                onSpeedChange = { speed -> updateSpeechRate(speed) },
                onPitchChange = { pitch -> updateVoicePitch(pitch) },
                onVoiceChange = { voiceName ->
                    selectedVoiceName.value = voiceName
                    saveSelectedVoice(voiceName)
                    ttsManager.setVoiceByName(voiceName)
                    statusText.value = "Voce selezionata"
                },
                onTestVoice = { ttsManager.testVoice(speechRate.value, voicePitch.value) }
            )
        }
    }

    override fun onDestroy() {
        ttsManager.shutdown()
        super.onDestroy()
    }

    private fun createOpenEpubIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/epub+zip"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/epub+zip", "application/octet-stream"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
    }

    private fun importEpub(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Some providers grant temporary read access without persistable permissions.
        }

        scope.launch {
            statusText.value = "Importazione EPUB..."
            val document = runCatching {
                withContext(Dispatchers.IO) { repository.loadEpub(uri) }
            }.getOrElse {
                statusText.value = "Impossibile importare EPUB"
                return@launch
            }

            if (document.chunks.isEmpty()) {
                statusText.value = "EPUB importato ma senza testo leggibile"
                return@launch
            }

            val configuredDocument = document.copy(speed = speechRate.value, pitch = voicePitch.value)
            documents.value = documents.value + configuredDocument
            current.value = configuredDocument
            textHighlight.value = TextHighlight()
            statusText.value = "Importato: ${configuredDocument.title}"
        }
    }

    private fun selectDocument(document: AudioDocument) {
        current.value = document
        isPlaying.value = false
        ttsManager.stop()
        textHighlight.value = TextHighlight()
        statusText.value = "Documento selezionato"
    }

    private fun togglePlay() {
        val document = current.value
        if (!ttsReady.value) {
            statusText.value = "Text-to-Speech non ancora pronto"
            return
        }
        if (document == null) {
            statusText.value = "Importa o seleziona un EPUB"
            return
        }

        if (isPlaying.value) {
            isPlaying.value = false
            ttsManager.stop()
            textHighlight.value = TextHighlight()
            statusText.value = "Pausa"
        } else {
            isPlaying.value = true
            statusText.value = "Lettura in corso"
            textHighlight.value = fallbackHighlightForCurrentSentence()
            ttsManager.speak(document)
        }
    }

    private fun stopReading() {
        isPlaying.value = false
        ttsManager.stop()
        current.value?.let { document ->
            updateCurrent { document.copy(index = 0) }
        }
        textHighlight.value = TextHighlight()
        statusText.value = "Lettura fermata"
    }

    private fun skip(delta: Int) {
        val document = current.value ?: return
        val max = (document.chunks.size - 1).coerceAtLeast(0)
        val newIndex = (document.index + delta).coerceIn(0, max)
        val updated = document.copy(index = newIndex)
        updateCurrent { updated }
        textHighlight.value = if (isPlaying.value) fallbackHighlightForText(updated.currentChunk) else TextHighlight()
        statusText.value = updated.progressLabel
        if (isPlaying.value) ttsManager.speak(updated)
    }

    private fun moveToNextChunk() {
        val document = current.value ?: return
        val nextIndex = document.index + 1
        if (nextIndex >= document.chunks.size) {
            isPlaying.value = false
            statusText.value = "Fine documento"
            return
        }

        val updated = document.copy(index = nextIndex)
        updateCurrent { updated }
        textHighlight.value = fallbackHighlightForText(updated.currentChunk)
        ttsManager.speak(updated)
    }

    private fun updateSpeechRate(speed: Float) {
        speechRate.value = speed
        preferences.edit().putFloat(KEY_SPEECH_RATE, speed).apply()
        updateCurrent { it.copy(speed = speed) }
    }

    private fun updateVoicePitch(pitch: Float) {
        voicePitch.value = pitch
        preferences.edit().putFloat(KEY_VOICE_PITCH, pitch).apply()
        updateCurrent { it.copy(pitch = pitch) }
    }

    private fun saveSelectedVoice(voiceName: String) {
        preferences.edit().putString(KEY_SELECTED_VOICE, voiceName).apply()
    }

    private fun updateCurrent(transform: (AudioDocument) -> AudioDocument) {
        val existing = current.value ?: return
        val updated = transform(existing)
        current.value = updated
        documents.value = documents.value.map { if (it.id == updated.id) updated else it }
    }

    private fun fallbackHighlightForCurrentSentence(): TextHighlight {
        return fallbackHighlightForText(current.value?.currentChunk.orEmpty())
    }

    private fun fallbackHighlightForText(text: String): TextHighlight {
        if (text.isBlank()) return TextHighlight()

        val match = Regex("[^.!?;:]+[.!?;:]?").find(text)
        val start = match?.range?.first ?: 0
        val end = (match?.range?.last?.plus(1) ?: text.length).coerceAtMost(text.length)

        return TextHighlight(start, end, exact = false)
    }

    companion object {
        private const val KEY_SELECTED_VOICE = "selected_voice"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_VOICE_PITCH = "voice_pitch"
    }
}
