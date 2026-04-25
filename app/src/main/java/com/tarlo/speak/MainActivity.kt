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
import com.tarlo.speak.model.HighlightMode
import com.tarlo.speak.model.ReaderSegment
import com.tarlo.speak.model.TextHighlight
import com.tarlo.speak.model.TtsProviderType
import com.tarlo.speak.model.TtsVoiceInfo
import com.tarlo.speak.model.VoicePreset
import com.tarlo.speak.tts.GoogleCloudTtsProvider
import com.tarlo.speak.tts.TtsManager
import com.tarlo.speak.ui.TarloSpeakApp
import com.tarlo.speak.ui.TarloUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var repository: DocumentRepository
    private lateinit var ttsManager: TtsManager
    private lateinit var preferences: SharedPreferences

    private val scope = CoroutineScope(Dispatchers.Main)
    private var nextSegmentJob: Job? = null
    private var currentSegmentStart = 0
    private var lastSpeechProvider = TtsProviderType.ANDROID

    private var documents = androidx.compose.runtime.mutableStateOf<List<AudioDocument>>(emptyList())
    private var current = androidx.compose.runtime.mutableStateOf<AudioDocument?>(null)
    private var isPlaying = androidx.compose.runtime.mutableStateOf(false)
    private var statusText = androidx.compose.runtime.mutableStateOf("Importa un EPUB per iniziare")
    private var ttsReady = androidx.compose.runtime.mutableStateOf(false)
    private var voices = androidx.compose.runtime.mutableStateOf<List<TtsVoiceInfo>>(emptyList())
    private var selectedVoiceName = androidx.compose.runtime.mutableStateOf<String?>(null)
    private var showAllVoices = androidx.compose.runtime.mutableStateOf(false)
    private var selectedPreset = androidx.compose.runtime.mutableStateOf(VoicePreset.AUDIOBOOK_PRO)
    private var customSettings = androidx.compose.runtime.mutableStateOf(false)
    private var speechRate = androidx.compose.runtime.mutableStateOf(0.90f)
    private var voicePitch = androidx.compose.runtime.mutableStateOf(1.0f)
    private var commaPauseMs = androidx.compose.runtime.mutableStateOf(140)
    private var periodPauseMs = androidx.compose.runtime.mutableStateOf(320)
    private var paragraphPauseMs = androidx.compose.runtime.mutableStateOf(650)
    private var pagePauseMs = androidx.compose.runtime.mutableStateOf(850)
    private var fontSizeSp = androidx.compose.runtime.mutableStateOf(20f)
    private var lineHeightMultiplier = androidx.compose.runtime.mutableStateOf(1.45f)
    private var highlightMode = androidx.compose.runtime.mutableStateOf(HighlightMode.SENTENCE)
    private var textHighlight = androidx.compose.runtime.mutableStateOf(TextHighlight())
    private var selectedProvider = androidx.compose.runtime.mutableStateOf(TtsProviderType.ANDROID)
    private var googleApiKey = androidx.compose.runtime.mutableStateOf("")
    private var googleApiKeyDraft = androidx.compose.runtime.mutableStateOf("")
    private var googleVoiceName = androidx.compose.runtime.mutableStateOf(GoogleCloudTtsProvider.DEFAULT_VOICE)
    private var googleCharsThisMonth = androidx.compose.runtime.mutableStateOf(0)
    private var googleCounterMonth = androidx.compose.runtime.mutableStateOf(currentMonthKey())
    private var googleDisabledForMonth = androidx.compose.runtime.mutableStateOf(false)
    private var apiKeyChangeConfirmation = androidx.compose.runtime.mutableStateOf(false)
    private var deleteApiKeyConfirmation = androidx.compose.runtime.mutableStateOf(false)
    private var resetCounterConfirmation = androidx.compose.runtime.mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = DocumentRepository(this)
        preferences = getSharedPreferences("tarlo_voice_settings", MODE_PRIVATE)
        loadSettings()

        ttsManager = TtsManager(
            context = this,
            preferredVoiceName = selectedVoiceName.value,
            onReadyChanged = { ready, availableVoices ->
                runOnUiThread {
                    ttsReady.value = ready
                    voices.value = filterVoices(availableVoices)
                    val selected = voices.value.firstOrNull { it.name == selectedVoiceName.value }
                        ?: voices.value.firstOrNull { it.isItalian }
                        ?: voices.value.firstOrNull()
                    selectedVoiceName.value = selected?.name
                    selected?.name?.let { saveSelectedVoice(it) }
                    statusText.value = if (ready) "Reader pronto" else "Text-to-Speech non disponibile"
                }
            },
            onChunkStarted = {
                runOnUiThread {
                    textHighlight.value = currentSegmentHighlight(exact = false)
                }
            },
            onChunkDone = {
                runOnUiThread {
                    if (isPlaying.value) scheduleNextSegment()
                }
            },
            onRangeStart = { start, end ->
                runOnUiThread {
                    if (highlightMode.value == HighlightMode.WORD) {
                        val pageLength = current.value?.currentPage?.length ?: 0
                        val safeStart = (currentSegmentStart + start).coerceIn(0, pageLength)
                        val safeEnd = (currentSegmentStart + end).coerceIn(safeStart, pageLength)
                        if (safeEnd > safeStart) {
                            textHighlight.value = TextHighlight(safeStart, safeEnd, exact = true)
                        }
                    }
                }
            },
            onGoogleCharactersWillBeSent = { characters -> reserveGoogleCharacters(characters) },
            onError = { message ->
                runOnUiThread { handleTtsError(message) }
            }
        )
        configureTtsProvider()

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
                    showAllVoices = showAllVoices.value,
                    selectedPreset = selectedPreset.value,
                    customSettings = customSettings.value,
                    speechRate = speechRate.value,
                    voicePitch = voicePitch.value,
                    commaPauseMs = commaPauseMs.value,
                    periodPauseMs = periodPauseMs.value,
                    paragraphPauseMs = paragraphPauseMs.value,
                    pagePauseMs = pagePauseMs.value,
                    fontSizeSp = fontSizeSp.value,
                    lineHeightMultiplier = lineHeightMultiplier.value,
                    highlightMode = highlightMode.value,
                    highlight = textHighlight.value,
                    selectedProvider = selectedProvider.value,
                    googleApiKeyDraft = googleApiKeyDraft.value,
                    maskedGoogleApiKey = maskApiKey(googleApiKey.value),
                    hasGoogleApiKey = googleApiKey.value.isNotBlank(),
                    googleVoiceName = googleVoiceName.value,
                    googleVoiceNames = GoogleCloudTtsProvider.ITALIAN_WAVENET_VOICES,
                    googleCharsThisMonth = googleCharsThisMonth.value,
                    googleMonthlyLimit = GOOGLE_MONTHLY_LIMIT,
                    googleSafetyLimit = GOOGLE_BLOCK_LIMIT,
                    googleSafetyPercent = googleSafetyPercent(),
                    googleBlocked = isGoogleBlocked(),
                    googleDisabledForMonth = googleDisabledForMonth.value,
                    googleWarningText = googleWarningText(),
                    apiKeyChangeConfirmation = apiKeyChangeConfirmation.value,
                    deleteApiKeyConfirmation = deleteApiKeyConfirmation.value,
                    resetCounterConfirmation = resetCounterConfirmation.value
                ),
                onImportClick = {
                    statusText.value = "Seleziona un file EPUB"
                    picker.launch(createOpenEpubIntent())
                },
                onSelectDocument = { selectDocument(it) },
                onTogglePlay = { togglePlay() },
                onStop = { stopReading() },
                onPreviousPage = { goToPage((current.value?.pageIndex ?: 0) - 1, restartReading = isPlaying.value) },
                onNextPage = { goToPage((current.value?.pageIndex ?: 0) + 1, restartReading = isPlaying.value) },
                onBeginning = { goToPage(0, restartReading = isPlaying.value) },
                onPresetChange = { applyPreset(it) },
                onRestorePreset = { applyPreset(selectedPreset.value) },
                onSavePersonalSettings = { savePersonalSettings() },
                onShowAllVoicesChange = { updateShowAllVoices(it) },
                onSpeedChange = { updateSpeechRate(it, custom = true) },
                onPitchChange = { updateVoicePitch(it, custom = true) },
                onCommaPauseChange = { updateCommaPause(it.toInt(), custom = true) },
                onPeriodPauseChange = { updatePeriodPause(it.toInt(), custom = true) },
                onParagraphPauseChange = { updateParagraphPause(it.toInt(), custom = true) },
                onPagePauseChange = { updatePagePause(it.toInt(), custom = true) },
                onFontSizeChange = { updateFontSize(it, custom = true) },
                onLineHeightChange = { updateLineHeight(it, custom = true) },
                onHighlightModeChange = { updateHighlightMode(it) },
                onVoiceChange = { voiceName ->
                    selectedVoiceName.value = voiceName
                    saveSelectedVoice(voiceName)
                    ttsManager.setVoiceByName(voiceName)
                    statusText.value = "Voce selezionata"
                },
                onTestVoice = { testSelectedVoice() },
                onProviderChange = { updateProvider(it) },
                onGoogleApiKeyDraftChange = { updateGoogleApiKeyDraft(it) },
                onSaveGoogleApiKey = { saveGoogleApiKeyWithConfirmation() },
                onDeleteGoogleApiKey = { deleteGoogleApiKeyWithConfirmation() },
                onUseAndroidForMonth = { useAndroidForRestOfMonth() },
                onGoogleVoiceChange = { updateGoogleVoice(it) },
                onTestPremiumVoice = { testPremiumVoice() },
                onResetGoogleCounter = { resetGoogleCounterWithConfirmation() }
            )
        }
    }

    override fun onDestroy() {
        nextSegmentJob?.cancel()
        ttsManager.shutdown()
        super.onDestroy()
    }

    private fun loadSettings() {
        selectedPreset.value = VoicePreset.fromName(preferences.getString(KEY_VOICE_PRESET, VoicePreset.AUDIOBOOK_PRO.name))
        customSettings.value = preferences.getBoolean(KEY_CUSTOM_SETTINGS, false)
        selectedVoiceName.value = preferences.getString(KEY_SELECTED_VOICE, null)
        showAllVoices.value = preferences.getBoolean(KEY_SHOW_ALL_VOICES, false)
        speechRate.value = preferences.getFloat(KEY_SPEECH_RATE, selectedPreset.value.rate)
        voicePitch.value = preferences.getFloat(KEY_VOICE_PITCH, selectedPreset.value.pitch)
        commaPauseMs.value = preferences.getInt(KEY_COMMA_PAUSE, selectedPreset.value.commaPauseMs)
        periodPauseMs.value = preferences.getInt(KEY_PERIOD_PAUSE, selectedPreset.value.periodPauseMs)
        paragraphPauseMs.value = preferences.getInt(KEY_PARAGRAPH_PAUSE, selectedPreset.value.paragraphPauseMs)
        pagePauseMs.value = preferences.getInt(KEY_PAGE_PAUSE, selectedPreset.value.pagePauseMs)
        fontSizeSp.value = preferences.getFloat(KEY_FONT_SIZE, selectedPreset.value.fontSizeSp)
        lineHeightMultiplier.value = preferences.getFloat(KEY_LINE_HEIGHT, selectedPreset.value.lineHeightMultiplier)
        highlightMode.value = HighlightMode.fromName(preferences.getString(KEY_HIGHLIGHT_MODE, HighlightMode.SENTENCE.name))
        selectedProvider.value = TtsProviderType.fromName(preferences.getString(KEY_SELECTED_PROVIDER, TtsProviderType.ANDROID.name))
        googleApiKey.value = preferences.getString(KEY_GOOGLE_API_KEY, "").orEmpty()
        googleApiKeyDraft.value = ""
        googleVoiceName.value = preferences.getString(KEY_GOOGLE_VOICE, GoogleCloudTtsProvider.DEFAULT_VOICE).orEmpty()
            .ifBlank { GoogleCloudTtsProvider.DEFAULT_VOICE }
        googleDisabledForMonth.value = preferences.getBoolean(KEY_GOOGLE_DISABLED_MONTH, false)
        loadGoogleUsage()
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
        }

        scope.launch {
            statusText.value = "Importazione EPUB..."
            val document = runCatching {
                withContext(Dispatchers.IO) { repository.loadEpub(uri, estimatedPageLength()) }
            }.getOrElse {
                statusText.value = "Impossibile importare EPUB"
                return@launch
            }

            if (document.pages.isEmpty()) {
                statusText.value = "EPUB importato ma senza testo leggibile"
                return@launch
            }

            val configured = document.copy(
                speed = speechRate.value,
                pitch = voicePitch.value,
                providerType = selectedProvider.value,
                presetName = selectedPreset.value.name
            )
            documents.value = documents.value + configured
            current.value = configured
            textHighlight.value = TextHighlight()
            statusText.value = "Importato: ${configured.title}"
        }
    }

    private fun selectDocument(document: AudioDocument) {
        isPlaying.value = false
        nextSegmentJob?.cancel()
        ttsManager.stop()
        current.value = document
        textHighlight.value = TextHighlight()
        statusText.value = document.progressLabel
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
            pauseReading()
        } else {
            isPlaying.value = true
            statusText.value = document.progressLabel
            speakCurrentSegment()
        }
    }

    private fun pauseReading() {
        isPlaying.value = false
        nextSegmentJob?.cancel()
        ttsManager.stop()
        statusText.value = "Pausa"
    }

    private fun stopReading() {
        isPlaying.value = false
        nextSegmentJob?.cancel()
        ttsManager.stop()
        updateCurrent { it.copy(pageIndex = 0, segmentIndex = 0) }
        textHighlight.value = TextHighlight()
        statusText.value = "Lettura fermata"
    }

    private fun goToPage(targetIndex: Int, restartReading: Boolean) {
        val document = current.value ?: return
        val max = (document.pages.size - 1).coerceAtLeast(0)
        val nextPage = targetIndex.coerceIn(0, max)
        nextSegmentJob?.cancel()
        ttsManager.stop()
        updateCurrent { it.copy(pageIndex = nextPage, segmentIndex = 0) }
        textHighlight.value = TextHighlight()
        statusText.value = current.value?.progressLabel ?: ""
        if (restartReading) {
            isPlaying.value = true
            speakCurrentSegment()
        }
    }

    private fun speakCurrentSegment() {
        val document = current.value ?: return
        val segments = currentSegments(document)
        if (segments.isEmpty()) {
            moveToNextPageOrFinish()
            return
        }

        val safeIndex = document.segmentIndex.coerceIn(0, segments.lastIndex)
        if (safeIndex != document.segmentIndex) updateCurrent { it.copy(segmentIndex = safeIndex) }
        val segment = segments[safeIndex]
        currentSegmentStart = segment.start
        textHighlight.value = currentSegmentHighlight(exact = false)
        val provider = resolveProviderForText(segment.text)
        lastSpeechProvider = provider
        configureTtsProvider(provider)
        ttsManager.speakText(
            text = cleanForSpeech(segment.text),
            speed = speechRate.value,
            pitch = voicePitch.value,
            utteranceId = "page-${document.id}-${document.pageIndex}-$safeIndex",
            preferredProvider = provider
        )
    }

    private fun scheduleNextSegment() {
        nextSegmentJob?.cancel()
        val document = current.value ?: return
        val segments = currentSegments(document)
        val segment = segments.getOrNull(document.segmentIndex)
        val delayMs = naturalPauseFor(segment)
        nextSegmentJob = scope.launch {
            delay(delayMs.toLong())
            if (isPlaying.value) moveToNextSegment()
        }
    }

    private fun moveToNextSegment() {
        val document = current.value ?: return
        val nextSegment = document.segmentIndex + 1
        val segments = currentSegments(document)
        if (nextSegment < segments.size) {
            updateCurrent { it.copy(segmentIndex = nextSegment, speed = speechRate.value, pitch = voicePitch.value) }
            speakCurrentSegment()
        } else {
            moveToNextPageOrFinish()
        }
    }

    private fun moveToNextPageOrFinish() {
        val document = current.value ?: return
        val nextPage = document.pageIndex + 1
        if (nextPage >= document.pages.size) {
            isPlaying.value = false
            textHighlight.value = TextHighlight()
            statusText.value = "Fine documento"
            return
        }

        updateCurrent { it.copy(pageIndex = nextPage, segmentIndex = 0) }
        textHighlight.value = TextHighlight()
        statusText.value = current.value?.progressLabel ?: ""
        nextSegmentJob?.cancel()
        nextSegmentJob = scope.launch {
            delay(pagePauseMs.value.toLong())
            if (isPlaying.value) speakCurrentSegment()
        }
    }

    private fun currentSegments(document: AudioDocument? = current.value): List<ReaderSegment> {
        val safeDocument = document ?: return emptyList()
        return splitIntoSegments(safeDocument.currentPage)
    }

    private fun splitIntoSegments(page: String): List<ReaderSegment> {
        if (page.isBlank()) return emptyList()
        val segments = mutableListOf<ReaderSegment>()
        Regex("[^\\n]+").findAll(page).forEach { paragraphMatch ->
            val paragraph = paragraphMatch.value
            Regex("[^.!?;:]+[.!?;:]?").findAll(paragraph).forEach { match ->
                val raw = match.value.trim()
                if (raw.length < 2) return@forEach
                val start = paragraphMatch.range.first + match.range.first + match.value.indexOf(raw)
                val end = (start + raw.length).coerceAtMost(page.length)
                segments += ReaderSegment(
                    text = raw,
                    start = start.coerceIn(0, page.length),
                    end = end,
                    paragraphEnd = match.range.last >= paragraph.length - 1
                )
            }
        }
        return segments.ifEmpty { listOf(ReaderSegment(page.trim(), 0, page.length, paragraphEnd = true)) }
    }

    private fun currentSegmentHighlight(exact: Boolean): TextHighlight {
        val document = current.value ?: return TextHighlight()
        val segment = currentSegments(document).getOrNull(document.segmentIndex) ?: return TextHighlight()
        return when (highlightMode.value) {
            HighlightMode.PARAGRAPH -> paragraphHighlight(document.currentPage, segment.start)
            else -> TextHighlight(segment.start, segment.end, exact = exact)
        }
    }

    private fun paragraphHighlight(page: String, position: Int): TextHighlight {
        val start = page.lastIndexOf("\n\n", position.coerceIn(0, page.length)).let { if (it < 0) 0 else it + 2 }
        val end = page.indexOf("\n\n", position.coerceIn(0, page.length)).let { if (it < 0) page.length else it }
        return TextHighlight(start, end.coerceAtLeast(start), exact = false)
    }

    private fun naturalPauseFor(segment: ReaderSegment?): Int {
        val text = segment?.text?.trim().orEmpty()
        val commaBonus = (text.count { it == ',' || it == ';' || it == ':' } * commaPauseMs.value).coerceAtMost(commaPauseMs.value * 3)
        val sentencePause = if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?")) periodPauseMs.value else 0
        val paragraphPause = if (segment?.paragraphEnd == true) paragraphPauseMs.value else 0
        return (commaBonus + sentencePause + paragraphPause).coerceIn(0, 2600)
    }

    private fun cleanForSpeech(text: String): String {
        return text
            .replace(Regex("\\b(Sig\\.ra|Sig|Dott|Prof|Ing)\\.")) { it.groupValues[1].replace(".", "") }
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun applyPreset(preset: VoicePreset) {
        selectedPreset.value = preset
        customSettings.value = false
        speechRate.value = preset.rate
        voicePitch.value = preset.pitch
        commaPauseMs.value = preset.commaPauseMs
        periodPauseMs.value = preset.periodPauseMs
        paragraphPauseMs.value = preset.paragraphPauseMs
        pagePauseMs.value = preset.pagePauseMs
        fontSizeSp.value = preset.fontSizeSp
        lineHeightMultiplier.value = preset.lineHeightMultiplier
        saveReadingSettings()
        updateCurrent { it.copy(speed = preset.rate, pitch = preset.pitch, presetName = preset.name) }
        statusText.value = "Preset ${preset.label} applicato"
    }

    private fun savePersonalSettings() {
        customSettings.value = true
        saveReadingSettings()
        statusText.value = "Impostazione personale salvata"
    }

    private fun updateSpeechRate(value: Float, custom: Boolean) {
        speechRate.value = value
        markCustom(custom)
        saveReadingSettings()
        updateCurrent { it.copy(speed = value) }
    }

    private fun updateVoicePitch(value: Float, custom: Boolean) {
        voicePitch.value = value
        markCustom(custom)
        saveReadingSettings()
        updateCurrent { it.copy(pitch = value) }
    }

    private fun updateCommaPause(value: Int, custom: Boolean) {
        commaPauseMs.value = value
        markCustom(custom)
        saveReadingSettings()
    }

    private fun updatePeriodPause(value: Int, custom: Boolean) {
        periodPauseMs.value = value
        markCustom(custom)
        saveReadingSettings()
    }

    private fun updateParagraphPause(value: Int, custom: Boolean) {
        paragraphPauseMs.value = value
        markCustom(custom)
        saveReadingSettings()
    }

    private fun updatePagePause(value: Int, custom: Boolean) {
        pagePauseMs.value = value
        markCustom(custom)
        saveReadingSettings()
    }

    private fun updateFontSize(value: Float, custom: Boolean) {
        fontSizeSp.value = value
        markCustom(custom)
        saveReadingSettings()
    }

    private fun updateLineHeight(value: Float, custom: Boolean) {
        lineHeightMultiplier.value = value
        markCustom(custom)
        saveReadingSettings()
    }

    private fun updateHighlightMode(mode: HighlightMode) {
        highlightMode.value = mode
        preferences.edit().putString(KEY_HIGHLIGHT_MODE, mode.name).apply()
        textHighlight.value = currentSegmentHighlight(exact = false)
    }

    private fun markCustom(custom: Boolean) {
        if (custom) customSettings.value = true
    }

    private fun saveReadingSettings() {
        preferences.edit()
            .putString(KEY_VOICE_PRESET, selectedPreset.value.name)
            .putBoolean(KEY_CUSTOM_SETTINGS, customSettings.value)
            .putFloat(KEY_SPEECH_RATE, speechRate.value)
            .putFloat(KEY_VOICE_PITCH, voicePitch.value)
            .putInt(KEY_COMMA_PAUSE, commaPauseMs.value)
            .putInt(KEY_PERIOD_PAUSE, periodPauseMs.value)
            .putInt(KEY_PARAGRAPH_PAUSE, paragraphPauseMs.value)
            .putInt(KEY_PAGE_PAUSE, pagePauseMs.value)
            .putFloat(KEY_FONT_SIZE, fontSizeSp.value)
            .putFloat(KEY_LINE_HEIGHT, lineHeightMultiplier.value)
            .apply()
    }

    private fun updateShowAllVoices(enabled: Boolean) {
        showAllVoices.value = enabled
        preferences.edit().putBoolean(KEY_SHOW_ALL_VOICES, enabled).apply()
        voices.value = filterVoices(ttsManager.availableVoices)
    }

    private fun updateProvider(provider: TtsProviderType) {
        if (provider == TtsProviderType.GOOGLE_CLOUD && isGoogleBlocked()) {
            selectedProvider.value = TtsProviderType.ANDROID
            preferences.edit().putString(KEY_SELECTED_PROVIDER, TtsProviderType.ANDROID.name).apply()
            configureTtsProvider(TtsProviderType.ANDROID)
            statusText.value = "Google WaveNet bloccato per sicurezza"
            return
        }
        selectedProvider.value = provider
        preferences.edit().putString(KEY_SELECTED_PROVIDER, provider.name).apply()
        configureTtsProvider()
        updateCurrent { it.copy(providerType = provider) }
        statusText.value = if (provider == TtsProviderType.GOOGLE_CLOUD) "Voce premium selezionata" else "Voce dispositivo selezionata"
    }

    private fun updateGoogleApiKeyDraft(key: String) {
        googleApiKeyDraft.value = key.trim()
        apiKeyChangeConfirmation.value = false
        deleteApiKeyConfirmation.value = false
    }

    private fun saveGoogleApiKeyWithConfirmation() {
        val newKey = googleApiKeyDraft.value.trim()
        if (newKey.isBlank()) {
            statusText.value = "Inserisci una API key Google Cloud da salvare"
            return
        }
        if (googleApiKey.value.isNotBlank() && googleApiKey.value != newKey && !apiKeyChangeConfirmation.value) {
            apiKeyChangeConfirmation.value = true
            statusText.value = "Conferma cambio chiave Google TTS attiva"
            return
        }

        // Salvare una API key nel client e accettabile per test/personale, ma non e ideale per distribuzione pubblica.
        googleApiKey.value = newKey
        googleApiKeyDraft.value = ""
        apiKeyChangeConfirmation.value = false
        deleteApiKeyConfirmation.value = false
        preferences.edit().putString(KEY_GOOGLE_API_KEY, newKey).apply()
        if (isPlaying.value) {
            selectedProvider.value = TtsProviderType.ANDROID
            preferences.edit().putString(KEY_SELECTED_PROVIDER, TtsProviderType.ANDROID.name).apply()
            ttsManager.stop()
            configureTtsProvider(TtsProviderType.ANDROID)
            speakCurrentSegment()
        } else {
            configureTtsProvider()
        }
        statusText.value = "API key Google TTS salvata localmente"
    }

    private fun deleteGoogleApiKeyWithConfirmation() {
        if (googleApiKey.value.isBlank()) {
            googleApiKeyDraft.value = ""
            statusText.value = "Nessuna API key salvata"
            return
        }
        if (!deleteApiKeyConfirmation.value) {
            deleteApiKeyConfirmation.value = true
            statusText.value = "Tocca ancora per cancellare la API key"
            return
        }

        googleApiKey.value = ""
        googleApiKeyDraft.value = ""
        deleteApiKeyConfirmation.value = false
        apiKeyChangeConfirmation.value = false
        selectedProvider.value = TtsProviderType.ANDROID
        preferences.edit()
            .remove(KEY_GOOGLE_API_KEY)
            .putString(KEY_SELECTED_PROVIDER, TtsProviderType.ANDROID.name)
            .apply()
        ttsManager.stop()
        configureTtsProvider(TtsProviderType.ANDROID)
        if (isPlaying.value) speakCurrentSegment()
        statusText.value = "API key cancellata. Uso la voce dispositivo."
    }

    private fun useAndroidForRestOfMonth() {
        googleDisabledForMonth.value = true
        selectedProvider.value = TtsProviderType.ANDROID
        preferences.edit()
            .putBoolean(KEY_GOOGLE_DISABLED_MONTH, true)
            .putString(KEY_SELECTED_PROVIDER, TtsProviderType.ANDROID.name)
            .apply()
        ttsManager.stop()
        configureTtsProvider(TtsProviderType.ANDROID)
        if (isPlaying.value) speakCurrentSegment()
        statusText.value = "Google WaveNet disattivato fino al prossimo mese"
    }

    private fun updateGoogleVoice(voice: String) {
        googleVoiceName.value = voice
        preferences.edit().putString(KEY_GOOGLE_VOICE, voice).apply()
        configureTtsProvider()
    }

    private fun testSelectedVoice() {
        if (isPlaying.value) pauseReading()
        statusText.value = "Test voce"
        ttsManager.testAndroidVoice(speechRate.value, voicePitch.value)
    }

    private fun testPremiumVoice() {
        val text = "Ciao, questa e la voce premium Google Cloud selezionata."
        if (!canUseGoogleForText(text)) return
        if (isPlaying.value) pauseReading()
        configureTtsProvider(TtsProviderType.GOOGLE_CLOUD)
        statusText.value = "Test voce premium"
        ttsManager.testGoogleVoice(speechRate.value, voicePitch.value)
    }

    private fun configureTtsProvider(provider: TtsProviderType = selectedProvider.value) {
        ttsManager.configureProvider(provider, googleApiKey.value, googleVoiceName.value)
    }

    private fun resolveProviderForText(text: String): TtsProviderType {
        if (selectedProvider.value != TtsProviderType.GOOGLE_CLOUD) return TtsProviderType.ANDROID
        if (!canUseGoogleForText(text)) return TtsProviderType.ANDROID
        return TtsProviderType.GOOGLE_CLOUD
    }

    private fun canUseGoogleForText(text: String): Boolean {
        refreshGoogleUsageMonthIfNeeded()
        if (googleDisabledForMonth.value) {
            statusText.value = "Google WaveNet disattivato per questo mese. Uso la voce dispositivo."
            return false
        }
        if (googleApiKey.value.isBlank()) {
            statusText.value = "Inserisci una API key Google Cloud per usare WaveNet."
            return false
        }
        val nextUsage = googleCharsThisMonth.value + text.length
        if (googleCharsThisMonth.value >= GOOGLE_BLOCK_LIMIT || nextUsage >= GOOGLE_BLOCK_LIMIT) {
            blockGoogleForSafety()
            return false
        }
        return true
    }

    private fun reserveGoogleCharacters(characters: Int): Boolean {
        refreshGoogleUsageMonthIfNeeded()
        val nextUsage = googleCharsThisMonth.value + characters
        if (nextUsage >= GOOGLE_BLOCK_LIMIT) {
            blockGoogleForSafety()
            return false
        }
        googleCharsThisMonth.value = nextUsage
        preferences.edit()
            .putString(KEY_GOOGLE_COUNTER_MONTH, googleCounterMonth.value)
            .putInt(KEY_GOOGLE_CHARS_MONTH, nextUsage)
            .apply()
        return true
    }

    private fun loadGoogleUsage() {
        googleCounterMonth.value = preferences.getString(KEY_GOOGLE_COUNTER_MONTH, currentMonthKey()).orEmpty()
            .ifBlank { currentMonthKey() }
        googleCharsThisMonth.value = preferences.getInt(KEY_GOOGLE_CHARS_MONTH, 0)
        refreshGoogleUsageMonthIfNeeded()
    }

    private fun refreshGoogleUsageMonthIfNeeded() {
        val now = currentMonthKey()
        if (googleCounterMonth.value != now) {
            googleCounterMonth.value = now
            googleCharsThisMonth.value = 0
            googleDisabledForMonth.value = false
            preferences.edit()
                .putString(KEY_GOOGLE_COUNTER_MONTH, now)
                .putInt(KEY_GOOGLE_CHARS_MONTH, 0)
                .putBoolean(KEY_GOOGLE_DISABLED_MONTH, false)
                .apply()
        }
    }

    private fun resetGoogleCounterWithConfirmation() {
        if (!resetCounterConfirmation.value) {
            resetCounterConfirmation.value = true
            statusText.value = "Solo test/debug: non usare il reset per aggirare limiti o costi inattesi"
            return
        }
        resetCounterConfirmation.value = false
        googleCounterMonth.value = currentMonthKey()
        googleCharsThisMonth.value = 0
        googleDisabledForMonth.value = false
        preferences.edit()
            .putString(KEY_GOOGLE_COUNTER_MONTH, googleCounterMonth.value)
            .putInt(KEY_GOOGLE_CHARS_MONTH, 0)
            .putBoolean(KEY_GOOGLE_DISABLED_MONTH, false)
            .apply()
        statusText.value = "Contatore Google Cloud azzerato"
    }

    private fun googleWarningText(): String? {
        val used = googleCharsThisMonth.value
        return when {
            isGoogleBlocked() -> "Google WaveNet bloccato per sicurezza"
            used >= GOOGLE_STRONG_WARNING_LIMIT -> "Hai raggiunto 900.000 caratteri Google questo mese. Per evitare costi, Google WaveNet verra bloccato a 950.000."
            used >= GOOGLE_WARNING_LIMIT -> "Hai raggiunto 850.000 caratteri Google questo mese. Sei vicino al limite sicurezza."
            else -> null
        }
    }

    private fun blockGoogleForSafety() {
        selectedProvider.value = TtsProviderType.ANDROID
        preferences.edit().putString(KEY_SELECTED_PROVIDER, TtsProviderType.ANDROID.name).apply()
        configureTtsProvider(TtsProviderType.ANDROID)
        statusText.value = "Limite sicurezza Google raggiunto. Per evitare costi, la lettura continua con la voce dispositivo."
    }

    private fun isGoogleBlocked(): Boolean {
        return googleDisabledForMonth.value || googleCharsThisMonth.value >= GOOGLE_BLOCK_LIMIT
    }

    private fun googleSafetyPercent(): Int {
        return ((googleCharsThisMonth.value.toFloat() / GOOGLE_BLOCK_LIMIT.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    private fun maskApiKey(key: String): String {
        if (key.isBlank()) return "Nessuna API key salvata"
        if (key.length <= 10) return "********"
        return key.take(6) + "***************" + key.takeLast(4)
    }

    private fun handleTtsError(message: String) {
        if (lastSpeechProvider == TtsProviderType.GOOGLE_CLOUD && isPlaying.value) {
            selectedProvider.value = TtsProviderType.ANDROID
            preferences.edit().putString(KEY_SELECTED_PROVIDER, TtsProviderType.ANDROID.name).apply()
            configureTtsProvider(TtsProviderType.ANDROID)
            statusText.value = "$message. Uso la voce dispositivo."
            speakCurrentSegment()
        } else {
            isPlaying.value = false
            statusText.value = message
        }
    }

    private fun updateCurrent(transform: (AudioDocument) -> AudioDocument) {
        val existing = current.value ?: return
        val updated = transform(existing)
        current.value = updated
        documents.value = documents.value.map { if (it.id == updated.id) updated else it }
    }

    private fun filterVoices(allVoices: List<TtsVoiceInfo>): List<TtsVoiceInfo> {
        val preferred = allVoices.filter { it.isItalian }
        return if (showAllVoices.value || preferred.isEmpty()) allVoices else preferred
    }

    private fun saveSelectedVoice(voiceName: String) {
        preferences.edit().putString(KEY_SELECTED_VOICE, voiceName).apply()
    }

    private fun estimatedPageLength(): Int {
        return repository.pageLengthFor(fontSizeSp.value, lineHeightMultiplier.value, selectedPreset.value.pageLengthBase)
    }

    companion object {
        private const val KEY_SELECTED_VOICE = "selected_voice"
        private const val KEY_SHOW_ALL_VOICES = "show_all_voices"
        private const val KEY_VOICE_PRESET = "voice_preset"
        private const val KEY_CUSTOM_SETTINGS = "custom_settings"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_VOICE_PITCH = "voice_pitch"
        private const val KEY_COMMA_PAUSE = "comma_pause"
        private const val KEY_PERIOD_PAUSE = "period_pause"
        private const val KEY_PARAGRAPH_PAUSE = "paragraph_pause"
        private const val KEY_PAGE_PAUSE = "page_pause"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_LINE_HEIGHT = "line_height"
        private const val KEY_HIGHLIGHT_MODE = "highlight_mode"
        private const val KEY_SELECTED_PROVIDER = "selected_provider"
        private const val KEY_GOOGLE_API_KEY = "google_api_key"
        private const val KEY_GOOGLE_VOICE = "google_voice"
        private const val KEY_GOOGLE_COUNTER_MONTH = "google_counter_month"
        private const val KEY_GOOGLE_CHARS_MONTH = "google_chars_month"
        private const val KEY_GOOGLE_DISABLED_MONTH = "google_disabled_month"
        private const val GOOGLE_MONTHLY_LIMIT = 1_000_000
        private const val GOOGLE_WARNING_LIMIT = 850_000
        private const val GOOGLE_STRONG_WARNING_LIMIT = 900_000
        private const val GOOGLE_BLOCK_LIMIT = 950_000

        private fun currentMonthKey(): String {
            return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        }
    }
}
