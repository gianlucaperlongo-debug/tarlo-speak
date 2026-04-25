package com.tarlo.speak.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarlo.speak.model.AudioCacheStats
import com.tarlo.speak.model.AudioDocument
import com.tarlo.speak.model.GoogleVoiceInfo
import com.tarlo.speak.model.HighlightMode
import com.tarlo.speak.model.TextHighlight
import com.tarlo.speak.model.TtsProviderType
import com.tarlo.speak.model.TtsVoiceInfo
import com.tarlo.speak.model.VoicePreset

data class TarloUiState(
    val documents: List<AudioDocument> = emptyList(),
    val current: AudioDocument? = null,
    val isPlaying: Boolean = false,
    val statusText: String = "Importa un EPUB per iniziare",
    val ttsReady: Boolean = false,
    val voices: List<TtsVoiceInfo> = emptyList(),
    val selectedVoiceName: String? = null,
    val showAllVoices: Boolean = false,
    val selectedPreset: VoicePreset = VoicePreset.AUDIOBOOK_PRO,
    val customSettings: Boolean = false,
    val speechRate: Float = 0.90f,
    val voicePitch: Float = 1.0f,
    val commaPauseMs: Int = 140,
    val periodPauseMs: Int = 320,
    val paragraphPauseMs: Int = 650,
    val pagePauseMs: Int = 850,
    val fontSizeSp: Float = 20f,
    val lineHeightMultiplier: Float = 1.45f,
    val highlightMode: HighlightMode = HighlightMode.SENTENCE,
    val highlight: TextHighlight = TextHighlight(),
    val selectedProvider: TtsProviderType = TtsProviderType.ANDROID,
    val googleApiKeyDraft: String = "",
    val maskedGoogleApiKey: String = "Nessuna API key salvata",
    val hasGoogleApiKey: Boolean = false,
    val googleVoiceName: String = "it-IT-Wavenet-A",
    val googleChirpVoiceName: String = "",
    val googleChirpVoices: List<GoogleVoiceInfo> = emptyList(),
    val googleWavenetVoices: List<GoogleVoiceInfo> = emptyList(),
    val googleVoiceNames: List<String> = emptyList(),
    val googleCharsThisMonth: Int = 0,
    val googleChirpCharsThisMonth: Int = 0,
    val googleWavenetCharsThisMonth: Int = 0,
    val googleMonthlyLimit: Int = 5_000_000,
    val chirpMonthlyLimit: Int = 1_000_000,
    val wavenetMonthlyLimit: Int = 4_000_000,
    val chirpSafetyLimit: Int = 900_000,
    val wavenetSafetyLimit: Int = 3_800_000,
    val chirpSafetyPercent: Int = 0,
    val wavenetSafetyPercent: Int = 0,
    val googleBlocked: Boolean = false,
    val chirpBlocked: Boolean = false,
    val wavenetBlocked: Boolean = false,
    val googleDisabledForMonth: Boolean = false,
    val chirpWarningText: String? = null,
    val wavenetWarningText: String? = null,
    val apiKeyChangeConfirmation: Boolean = false,
    val deleteApiKeyConfirmation: Boolean = false,
    val resetCounterConfirmation: Boolean = false,
    val audioCacheStats: AudioCacheStats = AudioCacheStats(),
    val precacheNextPage: Boolean = false,
    val clearAudioCacheConfirmation: Boolean = false,
    val creditSaverMode: Boolean = true,
    val onlyCacheMode: Boolean = false,
    val deleteDocumentConfirmationId: String? = null
)

private enum class AppTab(val label: String) {
    LIBRARY("Libreria"),
    READER("Reader"),
    VOICE("Voce"),
    CREDITS("Crediti"),
    SETTINGS("Impostazioni")
}

@Composable
fun TarloSpeakApp(
    state: TarloUiState,
    onImportClick: () -> Unit,
    onSelectDocument: (AudioDocument) -> Unit,
    onDeleteDocument: (AudioDocument) -> Unit,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onBeginning: () -> Unit,
    onPresetChange: (VoicePreset) -> Unit,
    onRestorePreset: () -> Unit,
    onSavePersonalSettings: () -> Unit,
    onShowAllVoicesChange: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onCommaPauseChange: (Float) -> Unit,
    onPeriodPauseChange: (Float) -> Unit,
    onParagraphPauseChange: (Float) -> Unit,
    onPagePauseChange: (Float) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onHighlightModeChange: (HighlightMode) -> Unit,
    onVoiceChange: (String) -> Unit,
    onTestVoice: () -> Unit,
    onProviderChange: (TtsProviderType) -> Unit,
    onGoogleApiKeyDraftChange: (String) -> Unit,
    onSaveGoogleApiKey: () -> Unit,
    onDeleteGoogleApiKey: () -> Unit,
    onUseAndroidForMonth: () -> Unit,
    onGoogleVoiceChange: (String) -> Unit,
    onGoogleChirpVoiceChange: (String) -> Unit,
    onRefreshGoogleVoices: () -> Unit,
    onUseWavenet: () -> Unit,
    onUseAndroid: () -> Unit,
    onTestChirpVoice: () -> Unit,
    onTestWavenetVoice: () -> Unit,
    onResetGoogleCounter: () -> Unit,
    onAudioCacheEnabledChange: (Boolean) -> Unit,
    onPrecacheNextPageChange: (Boolean) -> Unit,
    onAudioCacheMaxSizeChange: (Int) -> Unit,
    onRefreshAudioCache: () -> Unit,
    onClearAudioCache: () -> Unit,
    onCreditSaverModeChange: (Boolean) -> Unit,
    onOnlyCacheModeChange: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableStateOf(AppTab.LIBRARY) }

    MaterialTheme {
        Surface(color = TarloColors.Background, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                when (selectedTab) {
                    AppTab.LIBRARY -> LibraryScreen(state, onImportClick, onSelectDocument, onDeleteDocument) { selectedTab = AppTab.READER }
                    AppTab.READER -> ReaderScreen(state, onPreviousPage, onNextPage, onBeginning, onTogglePlay, onStop) { selectedTab = AppTab.VOICE }
                    AppTab.VOICE -> VoiceScreen(
                        state = state,
                        onProviderChange = onProviderChange,
                        onShowAllVoicesChange = onShowAllVoicesChange,
                        onVoiceChange = onVoiceChange,
                        onTestVoice = onTestVoice,
                        onRefreshGoogleVoices = onRefreshGoogleVoices,
                        onGoogleVoiceChange = onGoogleVoiceChange,
                        onGoogleChirpVoiceChange = onGoogleChirpVoiceChange,
                        onTestChirpVoice = onTestChirpVoice,
                        onTestWavenetVoice = onTestWavenetVoice,
                        onPresetChange = onPresetChange,
                        onRestorePreset = onRestorePreset,
                        onSavePersonalSettings = onSavePersonalSettings,
                        onSpeedChange = onSpeedChange,
                        onPitchChange = onPitchChange,
                        onCommaPauseChange = onCommaPauseChange,
                        onPeriodPauseChange = onPeriodPauseChange,
                        onParagraphPauseChange = onParagraphPauseChange,
                        onPagePauseChange = onPagePauseChange,
                        onFontSizeChange = onFontSizeChange,
                        onLineHeightChange = onLineHeightChange,
                        onHighlightModeChange = onHighlightModeChange
                    )
                    AppTab.CREDITS -> CreditsScreen(
                        state = state,
                        onGoogleApiKeyDraftChange = onGoogleApiKeyDraftChange,
                        onSaveGoogleApiKey = onSaveGoogleApiKey,
                        onDeleteGoogleApiKey = onDeleteGoogleApiKey,
                        onUseAndroidForMonth = onUseAndroidForMonth,
                        onUseWavenet = onUseWavenet,
                        onUseAndroid = onUseAndroid,
                        onTestChirpVoice = onTestChirpVoice,
                        onTestWavenetVoice = onTestWavenetVoice,
                        onResetGoogleCounter = onResetGoogleCounter
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        state = state,
                        onAudioCacheEnabledChange = onAudioCacheEnabledChange,
                        onPrecacheNextPageChange = onPrecacheNextPageChange,
                        onAudioCacheMaxSizeChange = onAudioCacheMaxSizeChange,
                        onRefreshAudioCache = onRefreshAudioCache,
                        onClearAudioCache = onClearAudioCache,
                        onCreditSaverModeChange = onCreditSaverModeChange,
                        onOnlyCacheModeChange = onOnlyCacheModeChange,
                        onRestorePreset = onRestorePreset
                    )
                }

                BottomTabs(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun ScreenScaffold(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 98.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = TarloColors.Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
                subtitle?.let { Text(it, color = TarloColors.Muted, fontSize = 13.sp) }
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }
    }
}

@Composable
private fun BottomTabs(selected: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(containerColor = TarloColors.Player, contentColor = TarloColors.PlayerText, modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        AppTab.values().forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            AppTab.LIBRARY -> Icons.Default.LibraryBooks
                            AppTab.READER -> Icons.Default.MenuBook
                            AppTab.VOICE -> Icons.Default.VolumeUp
                            AppTab.CREDITS -> Icons.Default.CreditCard
                            AppTab.SETTINGS -> Icons.Default.Settings
                        },
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label, fontSize = 10.sp, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun LibraryScreen(
    state: TarloUiState,
    onImportClick: () -> Unit,
    onSelectDocument: (AudioDocument) -> Unit,
    onDeleteDocument: (AudioDocument) -> Unit,
    openReader: () -> Unit
) {
    ScreenScaffold("Libreria", state.statusText) {
        Button(
            onClick = onImportClick,
            colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.documents.isEmpty()) "Importa il tuo primo EPUB" else "Importa EPUB", fontWeight = FontWeight.Bold)
        }

        if (state.documents.isEmpty()) {
            InfoCard("Inizia in tre passi", "Importa un EPUB, scegli una voce e lascia che cache e limiti proteggano i crediti Google. Puoi usare sempre Android TTS senza API key.")
        } else {
            state.documents.forEach { document ->
                Card(colors = CardDefaults.cardColors(containerColor = TarloColors.Surface), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(document.title, color = TarloColors.Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${document.progressLabel} - ${document.progressPercent}%", color = TarloColors.Muted, fontSize = 13.sp)
                            }
                            if (state.audioCacheStats.fileCount > 0) Text("Cache", color = TarloColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    onSelectDocument(document)
                                    openReader()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Apri") }
                            Button(
                                onClick = { onDeleteDocument(document) },
                                colors = ButtonDefaults.buttonColors(containerColor = TarloColors.SurfaceMuted, contentColor = TarloColors.Ink),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (state.deleteDocumentConfirmationId == document.id) "Conferma" else "Elimina", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderScreen(
    state: TarloUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onBeginning: () -> Unit,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    openVoice: () -> Unit
) {
    var focusMode by remember { mutableStateOf(false) }
    ScreenScaffold(state.current?.title ?: "Reader", state.current?.progressLabel ?: "Nessun documento aperto") {
        Card(
            colors = CardDefaults.cardColors(containerColor = TarloColors.Page),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (focusMode) 650.dp else 520.dp)
                .pointerInput(state.current?.id, state.current?.pageIndex) {
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount },
                        onDragEnd = {
                            when {
                                dragTotal > 90f -> onPreviousPage()
                                dragTotal < -90f -> onNextPage()
                            }
                        }
                    )
                }
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(providerBadge(state), color = TarloColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                HighlightedReaderText(
                    text = state.current?.currentPage?.takeIf { it.isNotBlank() } ?: "Apri un EPUB dalla Libreria per iniziare.",
                    highlight = state.highlight,
                    fontSize = state.fontSizeSp,
                    lineHeightMultiplier = state.lineHeightMultiplier
                )
            }
        }
        if (!focusMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CompactButton("Inizio", Icons.Default.FirstPage, onBeginning, Modifier.weight(1f), enabled = state.current != null)
                CompactButton("Prec.", Icons.Default.ArrowBack, onPreviousPage, Modifier.weight(1f), enabled = state.current != null)
                CompactButton("Succ.", Icons.Default.ArrowForward, onNextPage, Modifier.weight(1f), enabled = state.current != null)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onTogglePlay, enabled = state.current != null, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.isPlaying) "Pausa" else "Play")
                }
                Button(onClick = onStop, enabled = state.current != null, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.SurfaceMuted, contentColor = TarloColors.Ink), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Stop")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = openVoice, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Voce") }
                Button(onClick = { focusMode = true }, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Focus") }
            }
        } else {
            Button(onClick = { focusMode = false }, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Mostra controlli")
            }
        }
    }
}

@Composable
private fun VoiceScreen(
    state: TarloUiState,
    onProviderChange: (TtsProviderType) -> Unit,
    onShowAllVoicesChange: (Boolean) -> Unit,
    onVoiceChange: (String) -> Unit,
    onTestVoice: () -> Unit,
    onRefreshGoogleVoices: () -> Unit,
    onGoogleVoiceChange: (String) -> Unit,
    onGoogleChirpVoiceChange: (String) -> Unit,
    onTestChirpVoice: () -> Unit,
    onTestWavenetVoice: () -> Unit,
    onPresetChange: (VoicePreset) -> Unit,
    onRestorePreset: () -> Unit,
    onSavePersonalSettings: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onCommaPauseChange: (Float) -> Unit,
    onPeriodPauseChange: (Float) -> Unit,
    onParagraphPauseChange: (Float) -> Unit,
    onPagePauseChange: (Float) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onHighlightModeChange: (HighlightMode) -> Unit
) {
    var showVoices by remember { mutableStateOf(true) }
    var showManual by remember { mutableStateOf(false) }
    ScreenScaffold("Voce", "Provider, voci e preset lettura") {
        SectionCard("Provider") { ProviderPicker(state, onProviderChange) }
        SectionCard("Voci") {
            ToggleHeader("Mostra voci", showVoices) { showVoices = !showVoices }
            if (showVoices) {
                AndroidVoicePicker(state, onShowAllVoicesChange, onVoiceChange, onTestVoice)
                Button(onClick = onRefreshGoogleVoices, enabled = state.hasGoogleApiKey, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Text("Aggiorna voci Google") }
                GoogleVoiceList("Chirp 3 HD italiane", state.googleChirpVoices, state.googleChirpVoiceName, emptyList(), "Nessuna voce Chirp italiana trovata.", onGoogleChirpVoiceChange)
                GoogleVoiceList("WaveNet italiane", state.googleWavenetVoices, state.googleVoiceName, state.googleVoiceNames, "Nessuna voce WaveNet italiana trovata.", onGoogleVoiceChange)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onTestChirpVoice, enabled = state.hasGoogleApiKey && !state.chirpBlocked, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Test Chirp") }
                    Button(onClick = onTestWavenetVoice, enabled = state.hasGoogleApiKey && !state.wavenetBlocked, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Test WaveNet") }
                }
            }
        }
        SectionCard("Preset") { Presets(state, onPresetChange, onRestorePreset, onSavePersonalSettings) }
        SectionCard("Regolazioni manuali") {
            ToggleHeader("Avanzate", showManual) { showManual = !showManual }
            if (showManual) {
                Text("Cambiare voce, provider, preset, velocita o pitch puo rigenerare audio e consumare caratteri Google.", color = TarloColors.Muted, fontSize = 12.sp)
                SliderRow("Velocita", "${"%.2f".format(state.speechRate)}x", state.speechRate, 0.70f..1.40f, onSpeedChange)
                SliderRow("Pitch", "%.2f".format(state.voicePitch), state.voicePitch, 0.80f..1.20f, onPitchChange)
                SliderRow("Pausa virgola", "${state.commaPauseMs} ms", state.commaPauseMs.toFloat(), 0f..400f, onCommaPauseChange)
                SliderRow("Pausa punto", "${state.periodPauseMs} ms", state.periodPauseMs.toFloat(), 100f..700f, onPeriodPauseChange)
                SliderRow("Pausa paragrafo", "${state.paragraphPauseMs} ms", state.paragraphPauseMs.toFloat(), 200f..1200f, onParagraphPauseChange)
                SliderRow("Pausa pagina", "${state.pagePauseMs} ms", state.pagePauseMs.toFloat(), 300f..1500f, onPagePauseChange)
                SliderRow("Font reader", "${state.fontSizeSp.toInt()} sp", state.fontSizeSp, 16f..28f, onFontSizeChange)
                SliderRow("Interlinea", "%.2f".format(state.lineHeightMultiplier), state.lineHeightMultiplier, 1.15f..1.80f, onLineHeightChange)
                HighlightPicker(state, onHighlightModeChange)
            }
        }
    }
}

@Composable
private fun CreditsScreen(
    state: TarloUiState,
    onGoogleApiKeyDraftChange: (String) -> Unit,
    onSaveGoogleApiKey: () -> Unit,
    onDeleteGoogleApiKey: () -> Unit,
    onUseAndroidForMonth: () -> Unit,
    onUseWavenet: () -> Unit,
    onUseAndroid: () -> Unit,
    onTestChirpVoice: () -> Unit,
    onTestWavenetVoice: () -> Unit,
    onResetGoogleCounter: () -> Unit
) {
    ScreenScaffold("Crediti", "Quote Google, API key e sicurezza") {
        SectionCard("Quota Google") {
            CreditLine("Chirp 3 HD", state.googleChirpCharsThisMonth, state.chirpMonthlyLimit, state.chirpSafetyLimit, providerStatus(state.chirpBlocked, state.chirpWarningText))
            CreditLine("WaveNet", state.googleWavenetCharsThisMonth, state.wavenetMonthlyLimit, state.wavenetSafetyLimit, providerStatus(state.wavenetBlocked, state.wavenetWarningText))
            Text("Totale Google questo mese: ${state.googleCharsThisMonth} / ${state.googleMonthlyLimit}", color = TarloColors.Muted, fontSize = 13.sp)
            Text("La cache evita nuove chiamate Google quando riascolti lo stesso testo con la stessa voce.", color = TarloColors.Muted, fontSize = 12.sp)
        }
        SectionCard("Google TTS API Key") {
            Text("Chiave attiva: ${state.maskedGoogleApiKey}", color = TarloColors.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            OutlinedTextField(
                value = state.googleApiKeyDraft,
                onValueChange = onGoogleApiKeyDraftChange,
                label = { Text("Nuova API key Google Cloud TTS") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.apiKeyChangeConfirmation) Text("Il contatore mensile dell'app non verra azzerato automaticamente.", color = TarloColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSaveGoogleApiKey, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text(if (state.apiKeyChangeConfirmation) "Conferma" else "Salva") }
                Button(onClick = onDeleteGoogleApiKey, enabled = state.hasGoogleApiKey, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.SurfaceMuted, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text(if (state.deleteApiKeyConfirmation) "Conferma" else "Cancella") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onTestChirpVoice, enabled = state.hasGoogleApiKey && !state.chirpBlocked, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Test Chirp") }
                Button(onClick = onTestWavenetVoice, enabled = state.hasGoogleApiKey && !state.wavenetBlocked, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Test WaveNet") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onUseWavenet, enabled = !state.wavenetBlocked, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Usa WaveNet") }
                Button(onClick = onUseAndroid, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Usa Android") }
            }
            Button(onClick = onUseAndroidForMonth, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Text("Usa Android TTS per il resto del mese") }
            Button(onClick = onResetGoogleCounter, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.SurfaceMuted, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Text(if (state.resetCounterConfirmation) "Conferma reset debug" else "Reset contatore solo test/debug") }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: TarloUiState,
    onAudioCacheEnabledChange: (Boolean) -> Unit,
    onPrecacheNextPageChange: (Boolean) -> Unit,
    onAudioCacheMaxSizeChange: (Int) -> Unit,
    onRefreshAudioCache: () -> Unit,
    onClearAudioCache: () -> Unit,
    onCreditSaverModeChange: (Boolean) -> Unit,
    onOnlyCacheModeChange: (Boolean) -> Unit,
    onRestorePreset: () -> Unit
) {
    ScreenScaffold("Impostazioni", "Cache, privacy e preferenze") {
        SectionCard("Risparmio crediti") {
            ToggleRow("Modalita risparmio crediti", "Usa cache prima di Google e riduce consumi anticipati.", state.creditSaverMode, onCreditSaverModeChange)
            ToggleRow("Solo audio gia salvato", "Se manca cache, passa ad Android senza chiamare Google.", state.onlyCacheMode, onOnlyCacheModeChange)
        }
        SectionCard("Cache audio") {
            AudioCacheSection(state, onAudioCacheEnabledChange, onPrecacheNextPageChange, onAudioCacheMaxSizeChange, onRefreshAudioCache, onClearAudioCache)
        }
        SectionCard("App") {
            Text("Tema: sistema", color = TarloColors.Muted, fontSize = 13.sp)
            Text("Lingua interfaccia: italiano", color = TarloColors.Muted, fontSize = 13.sp)
            Button(onClick = onRestorePreset, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Text("Reset impostazioni lettura consigliate") }
            Text("Privacy: la API key resta salvata solo localmente. La cache audio non contiene API key nei nomi file.", color = TarloColors.Muted, fontSize = 12.sp)
            Text("Tarlo Speak 1.0", color = TarloColors.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HighlightedReaderText(text: String, highlight: TextHighlight, fontSize: Float, lineHeightMultiplier: Float) {
    val safeStart = highlight.start.coerceIn(0, text.length)
    val safeEnd = highlight.end.coerceIn(safeStart, text.length)
    val annotated = buildAnnotatedString {
        append(text.substring(0, safeStart))
        if (safeEnd > safeStart) {
            withStyle(SpanStyle(background = if (highlight.exact) TarloColors.Highlight else TarloColors.HighlightSoft, color = TarloColors.Ink, fontWeight = FontWeight.SemiBold)) {
                append(text.substring(safeStart, safeEnd))
            }
        }
        append(text.substring(safeEnd))
    }
    Text(text = annotated, color = TarloColors.Ink, fontSize = fontSize.sp, lineHeight = (fontSize * lineHeightMultiplier).sp)
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = TarloColors.Surface), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = TarloColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    SectionCard(title) { Text(body, color = TarloColors.Muted, fontSize = 14.sp) }
}

@Composable
private fun ToggleHeader(label: String, expanded: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TarloColors.Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(if (expanded) "Nascondi" else "Mostra", color = TarloColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TarloColors.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = TarloColors.Muted, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun CompactButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun ProviderPicker(state: TarloUiState, onProviderChange: (TtsProviderType) -> Unit) {
    TtsProviderType.values().toList().chunked(1).forEach { rowItems ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            rowItems.forEach { provider ->
                val selected = provider == state.selectedProvider
                Button(
                    onClick = { onProviderChange(provider) },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selected) TarloColors.Accent else TarloColors.AccentSoft, contentColor = if (selected) TarloColors.PlayerText else TarloColors.Ink),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) { Text(provider.label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
private fun AndroidVoicePicker(state: TarloUiState, onShowAllVoicesChange: (Boolean) -> Unit, onVoiceChange: (String) -> Unit, onTestVoice: () -> Unit) {
    ToggleRow("Voci Android locali", "Mostra tutte le voci installate", state.showAllVoices, onShowAllVoicesChange)
    if (state.voices.isEmpty()) {
        Text("Installa altre voci TTS dalle impostazioni Android", color = TarloColors.Muted, fontSize = 13.sp)
    } else {
        LazyColumn(modifier = Modifier.height(if (state.voices.size == 1) 96.dp else 160.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.voices) { voice ->
                SelectButton(voice.name, "${voice.displayLanguage} - ${voice.quality} - ${voice.latency}", voice.name == state.selectedVoiceName) { onVoiceChange(voice.name) }
            }
        }
    }
    Button(onClick = onTestVoice, enabled = state.ttsReady, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Text("Test Android breve") }
}

@Composable
private fun GoogleVoiceList(title: String, voices: List<GoogleVoiceInfo>, selectedName: String, fallbackNames: List<String>, emptyMessage: String, onSelect: (String) -> Unit) {
    Text(title, color = TarloColors.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    val names = if (voices.isEmpty()) fallbackNames else emptyList()
    if (voices.isEmpty()) Text(emptyMessage, color = TarloColors.Muted, fontSize = 12.sp)
    if (voices.isNotEmpty()) {
        LazyColumn(modifier = Modifier.height(if (voices.size == 1) 82.dp else 150.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(voices) { voice -> SelectButton(voice.name, "${voice.displayLanguage} - ${voice.ssmlGender} - ${voice.sampleRateLabel}", voice.name == selectedName) { onSelect(voice.name) } }
        }
    } else {
        names.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { voice -> SelectButton(voice, "", voice == selectedName, Modifier.weight(1f)) { onSelect(voice) } }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SelectButton(title: String, subtitle: String, selected: Boolean, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = if (selected) TarloColors.Accent else TarloColors.AccentSoft, contentColor = if (selected) TarloColors.PlayerText else TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Presets(state: TarloUiState, onPresetChange: (VoicePreset) -> Unit, onRestorePreset: () -> Unit, onSavePersonalSettings: () -> Unit) {
    VoicePreset.values().toList().chunked(2).forEach { rowItems ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            rowItems.forEach { preset ->
                val selected = preset == state.selectedPreset && !state.customSettings
                Button(onClick = { onPresetChange(preset) }, colors = ButtonDefaults.buttonColors(containerColor = if (selected) TarloColors.Accent else TarloColors.AccentSoft, contentColor = if (selected) TarloColors.PlayerText else TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                    Text(preset.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onRestorePreset, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Ripristina") }
        Button(onClick = onSavePersonalSettings, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Salva personale") }
    }
}

@Composable
private fun HighlightPicker(state: TarloUiState, onHighlightModeChange: (HighlightMode) -> Unit) {
    Text("Evidenziazione", color = TarloColors.Ink, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        HighlightMode.values().forEach { mode ->
            Button(onClick = { onHighlightModeChange(mode) }, colors = ButtonDefaults.buttonColors(containerColor = if (mode == state.highlightMode) TarloColors.Accent else TarloColors.AccentSoft, contentColor = if (mode == state.highlightMode) TarloColors.PlayerText else TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                Text(mode.label, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row {
            Text(label, color = TarloColors.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(valueLabel, color = TarloColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = TarloColors.Accent, activeTrackColor = TarloColors.Accent, inactiveTrackColor = TarloColors.SurfaceMuted))
    }
}

@Composable
private fun AudioCacheSection(
    state: TarloUiState,
    onAudioCacheEnabledChange: (Boolean) -> Unit,
    onPrecacheNextPageChange: (Boolean) -> Unit,
    onAudioCacheMaxSizeChange: (Int) -> Unit,
    onRefreshAudioCache: () -> Unit,
    onClearAudioCache: () -> Unit
) {
    val stats = state.audioCacheStats
    ToggleRow("Usa cache audio", "Risparmia crediti quando riascolti lo stesso testo.", stats.enabled, onAudioCacheEnabledChange)
    ToggleRow("Precarica pagina successiva", "Puo consumare caratteri Google prima dell'ascolto.", state.precacheNextPage, onPrecacheNextPageChange)
    Text("Spazio: ${stats.sizeLabel} - ${stats.fileCount} file audio", color = TarloColors.Muted, fontSize = 12.sp)
    Text("Cache hit: ${stats.hitCount} segmenti - caratteri risparmiati: ${stats.savedCharacters}", color = TarloColors.Muted, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(100, 250, 500, 1024).forEach { size ->
            Button(onClick = { onAudioCacheMaxSizeChange(size) }, colors = ButtonDefaults.buttonColors(containerColor = if (stats.maxSizeMb == size) TarloColors.Accent else TarloColors.AccentSoft, contentColor = if (stats.maxSizeMb == size) TarloColors.PlayerText else TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                Text(if (size == 1024) "1 GB" else "$size MB", fontSize = 11.sp, maxLines = 1)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onRefreshAudioCache, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Ricontrolla", fontSize = 12.sp) }
        Button(onClick = onClearAudioCache, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.SurfaceMuted, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text(if (state.clearAudioCacheConfirmation) "Conferma svuota" else "Svuota cache", fontSize = 12.sp) }
    }
    if (state.clearAudioCacheConfirmation) Text("Svuotando la cache, le pagine gia generate consumeranno nuovamente caratteri Google.", color = TarloColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun CreditLine(label: String, used: Int, limit: Int, safety: Int, status: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$label: $used / $limit", color = TarloColors.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("Soglia blocco: $safety - stato: $status", color = TarloColors.Muted, fontSize = 12.sp)
    }
}

private fun providerStatus(blocked: Boolean, warning: String?): String = when {
    blocked -> "bloccato"
    warning != null -> "vicino al limite"
    else -> "attivo"
}

private fun providerBadge(state: TarloUiState): String {
    val provider = when (state.selectedProvider) {
        TtsProviderType.GOOGLE_CHIRP -> "Chirp 3 HD"
        TtsProviderType.GOOGLE_WAVENET -> "WaveNet"
        TtsProviderType.ANDROID -> "Android"
    }
    val cache = if (state.statusText.startsWith("Audio letto da cache")) " cache" else ""
    val fallback = if (state.statusText.contains("fallback", ignoreCase = true)) " fallback" else ""
    return "$provider$cache$fallback - ${state.selectedPreset.label}"
}
