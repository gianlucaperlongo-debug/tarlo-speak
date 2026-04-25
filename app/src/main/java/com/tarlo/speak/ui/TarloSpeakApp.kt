package com.tarlo.speak.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import com.tarlo.speak.model.AudioDocument
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
    val googleVoiceNames: List<String> = emptyList(),
    val googleCharsThisMonth: Int = 0,
    val googleMonthlyLimit: Int = 1_000_000,
    val googleSafetyLimit: Int = 950_000,
    val googleSafetyPercent: Int = 0,
    val googleBlocked: Boolean = false,
    val googleDisabledForMonth: Boolean = false,
    val googleWarningText: String? = null,
    val apiKeyChangeConfirmation: Boolean = false,
    val deleteApiKeyConfirmation: Boolean = false,
    val resetCounterConfirmation: Boolean = false
)

@Composable
fun TarloSpeakApp(
    state: TarloUiState,
    onImportClick: () -> Unit,
    onSelectDocument: (AudioDocument) -> Unit,
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
    onTestPremiumVoice: () -> Unit,
    onResetGoogleCounter: () -> Unit
) {
    MaterialTheme {
        Surface(color = TarloColors.Background, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 128.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Header(state, onImportClick) }
                    item { ReaderPage(state, onPreviousPage, onNextPage) }
                    item {
                        PageControls(
                            state = state,
                            onPreviousPage = onPreviousPage,
                            onNextPage = onNextPage,
                            onBeginning = onBeginning
                        )
                    }
                    item {
                        VoiceSettings(
                            state = state,
                            onPresetChange = onPresetChange,
                            onRestorePreset = onRestorePreset,
                            onSavePersonalSettings = onSavePersonalSettings,
                            onShowAllVoicesChange = onShowAllVoicesChange,
                            onSpeedChange = onSpeedChange,
                            onPitchChange = onPitchChange,
                            onCommaPauseChange = onCommaPauseChange,
                            onPeriodPauseChange = onPeriodPauseChange,
                            onParagraphPauseChange = onParagraphPauseChange,
                            onPagePauseChange = onPagePauseChange,
                            onFontSizeChange = onFontSizeChange,
                            onLineHeightChange = onLineHeightChange,
                            onHighlightModeChange = onHighlightModeChange,
                            onVoiceChange = onVoiceChange,
                            onTestVoice = onTestVoice,
                            onProviderChange = onProviderChange,
                            onGoogleApiKeyDraftChange = onGoogleApiKeyDraftChange,
                            onSaveGoogleApiKey = onSaveGoogleApiKey,
                            onDeleteGoogleApiKey = onDeleteGoogleApiKey,
                            onUseAndroidForMonth = onUseAndroidForMonth,
                            onGoogleVoiceChange = onGoogleVoiceChange,
                            onTestPremiumVoice = onTestPremiumVoice,
                            onResetGoogleCounter = onResetGoogleCounter
                        )
                    }
                    item { DocumentList(state, onSelectDocument) }
                }

                PlayerBar(
                    state = state,
                    onTogglePlay = onTogglePlay,
                    onStop = onStop,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun Header(state: TarloUiState, onImportClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(TarloColors.Accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TarloColors.PlayerText)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(state.current?.title ?: "Tarlo Speak", color = TarloColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.current?.progressLabel ?: state.statusText, color = TarloColors.Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Button(
            onClick = onImportClick,
            colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Importa EPUB", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReaderPage(state: TarloUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit) {
    val pageText = state.current?.currentPage?.takeIf { it.isNotBlank() } ?: "Importa un EPUB per vedere qui la pagina di lettura."
    val dragTotal = remember { mutableFloatStateOf(0f) }

    Card(
        colors = CardDefaults.cardColors(containerColor = TarloColors.Page),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(560.dp)
            .pointerInput(state.current?.id, state.current?.pageIndex) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal.floatValue = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragTotal.floatValue += dragAmount },
                    onDragEnd = {
                        when {
                            dragTotal.floatValue > 90f -> onPreviousPage()
                            dragTotal.floatValue < -90f -> onNextPage()
                        }
                    }
                )
            }
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp)) {
            Text(
                state.current?.progressLabel ?: "Pagina",
                color = TarloColors.Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            HighlightedReaderText(
                text = pageText,
                highlight = state.highlight,
                fontSize = state.fontSizeSp,
                lineHeightMultiplier = state.lineHeightMultiplier
            )
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
            withStyle(
                SpanStyle(
                    background = if (highlight.exact) TarloColors.Highlight else TarloColors.HighlightSoft,
                    color = TarloColors.Ink,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(text.substring(safeStart, safeEnd))
            }
        }
        append(text.substring(safeEnd))
    }

    Text(
        text = annotated,
        color = TarloColors.Ink,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * lineHeightMultiplier).sp
    )
}

@Composable
private fun PageControls(state: TarloUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit, onBeginning: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        CompactButton("Inizio", Icons.Default.FirstPage, onBeginning, Modifier.weight(1f))
        CompactButton("Precedente", Icons.Default.ArrowBack, onPreviousPage, Modifier.weight(1f), enabled = state.current != null)
        CompactButton("Successiva", Icons.Default.ArrowForward, onNextPage, Modifier.weight(1f), enabled = state.current != null)
    }
}

@Composable
private fun CompactButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun VoiceSettings(
    state: TarloUiState,
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
    onTestPremiumVoice: () -> Unit,
    onResetGoogleCounter: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = TarloColors.Surface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Voce / Lettura", color = TarloColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (state.customSettings) "Personalizzato" else state.selectedPreset.label, color = TarloColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            ProviderPicker(state, onProviderChange)
            AndroidVoicePicker(state, onShowAllVoicesChange, onVoiceChange, onTestVoice)
            GoogleVoicePicker(
                state = state,
                onGoogleApiKeyDraftChange = onGoogleApiKeyDraftChange,
                onSaveGoogleApiKey = onSaveGoogleApiKey,
                onDeleteGoogleApiKey = onDeleteGoogleApiKey,
                onUseAndroidForMonth = onUseAndroidForMonth,
                onGoogleVoiceChange = onGoogleVoiceChange,
                onTestPremiumVoice = onTestPremiumVoice,
                onResetGoogleCounter = onResetGoogleCounter
            )
            Presets(state, onPresetChange, onRestorePreset, onSavePersonalSettings)

            SliderRow("Velocita lettura", "${"%.2f".format(state.speechRate)}x", state.speechRate, 0.70f..1.40f, onSpeedChange)
            SliderRow("Tono / pitch", "%.2f".format(state.voicePitch), state.voicePitch, 0.80f..1.20f, onPitchChange)
            SliderRow("Pausa virgola", "${state.commaPauseMs} ms", state.commaPauseMs.toFloat(), 0f..400f, onCommaPauseChange)
            SliderRow("Pausa punto", "${state.periodPauseMs} ms", state.periodPauseMs.toFloat(), 100f..700f, onPeriodPauseChange)
            SliderRow("Pausa fine paragrafo", "${state.paragraphPauseMs} ms", state.paragraphPauseMs.toFloat(), 200f..1200f, onParagraphPauseChange)
            SliderRow("Pausa cambio pagina", "${state.pagePauseMs} ms", state.pagePauseMs.toFloat(), 300f..1500f, onPagePauseChange)
            SliderRow("Dimensione font reader", "${state.fontSizeSp.toInt()} sp", state.fontSizeSp, 16f..28f, onFontSizeChange)
            SliderRow("Interlinea", "%.2f".format(state.lineHeightMultiplier), state.lineHeightMultiplier, 1.15f..1.80f, onLineHeightChange)

            Text("Evidenziazione", color = TarloColors.Ink, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                HighlightMode.values().forEach { mode ->
                    val selected = mode == state.highlightMode
                    Button(
                        onClick = { onHighlightModeChange(mode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) TarloColors.Accent else TarloColors.AccentSoft,
                            contentColor = if (selected) TarloColors.PlayerText else TarloColors.Ink
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(mode.label, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderPicker(state: TarloUiState, onProviderChange: (TtsProviderType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TtsProviderType.values().forEach { provider ->
            val selected = provider == state.selectedProvider
            Button(
                onClick = { onProviderChange(provider) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) TarloColors.Accent else TarloColors.AccentSoft,
                    contentColor = if (selected) TarloColors.PlayerText else TarloColors.Ink
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(provider.label, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AndroidVoicePicker(state: TarloUiState, onShowAllVoicesChange: (Boolean) -> Unit, onVoiceChange: (String) -> Unit, onTestVoice: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Voci Android locali", color = TarloColors.Ink, fontWeight = FontWeight.Bold)
            Text("Mostra tutte le voci installate", color = TarloColors.Muted, fontSize = 12.sp)
        }
        Switch(checked = state.showAllVoices, onCheckedChange = onShowAllVoicesChange)
    }

    if (state.voices.isEmpty()) {
        Text("Installa altre voci TTS dalle impostazioni Android", color = TarloColors.Muted, fontSize = 13.sp)
    } else {
        LazyColumn(modifier = Modifier.height(if (state.voices.size == 1) 96.dp else 180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.voices) { voice ->
                val selected = voice.name == state.selectedVoiceName
                Button(
                    onClick = { onVoiceChange(voice.name) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) TarloColors.Accent else TarloColors.AccentSoft,
                        contentColor = if (selected) TarloColors.PlayerText else TarloColors.Ink
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(voice.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${voice.displayLanguage} - ${voice.quality} - ${voice.latency}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (state.voices.size == 1) {
            Text("Puoi installare altre voci dalle impostazioni di sintesi vocale Android", color = TarloColors.Muted, fontSize = 12.sp)
        }
    }

    Button(
        onClick = onTestVoice,
        enabled = state.ttsReady,
        colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.VolumeUp, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Test voce")
    }
}

@Composable
private fun GoogleVoicePicker(
    state: TarloUiState,
    onGoogleApiKeyDraftChange: (String) -> Unit,
    onSaveGoogleApiKey: () -> Unit,
    onDeleteGoogleApiKey: () -> Unit,
    onUseAndroidForMonth: () -> Unit,
    onGoogleVoiceChange: (String) -> Unit,
    onTestPremiumVoice: () -> Unit,
    onResetGoogleCounter: () -> Unit
) {
    Text("Google TTS API Key", color = TarloColors.Ink, fontWeight = FontWeight.Bold)
    Text("Una sola API key attiva. La chiave resta salvata solo su questo dispositivo.", color = TarloColors.Muted, fontSize = 12.sp)
    Text("Chiave attiva: ${state.maskedGoogleApiKey}", color = TarloColors.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text("Provider attivo: ${state.selectedProvider.label}", color = TarloColors.Muted, fontSize = 12.sp)
    Text(
        when {
            state.googleDisabledForMonth -> "Google WaveNet disattivato per il resto del mese"
            state.googleBlocked -> "Google WaveNet bloccato per sicurezza"
            else -> "Google WaveNet disponibile se la chiave e valida"
        },
        color = if (state.googleBlocked) TarloColors.Accent else TarloColors.Muted,
        fontSize = 12.sp,
        fontWeight = if (state.googleBlocked) FontWeight.Bold else FontWeight.Normal
    )

    OutlinedTextField(
        value = state.googleApiKeyDraft,
        onValueChange = onGoogleApiKeyDraftChange,
        label = { Text("Nuova API key Google Cloud TTS") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    if (state.apiKeyChangeConfirmation) {
        Text(
            "Stai cambiando la chiave Google TTS attiva. Assicurati che l'uso sia conforme ai termini del provider. Il contatore mensile dell'app non verra azzerato automaticamente.",
            color = TarloColors.Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onSaveGoogleApiKey,
            colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f)
        ) { Text(if (state.apiKeyChangeConfirmation) "Conferma cambio" else "Salva API key", fontSize = 13.sp) }
        Button(
            onClick = onDeleteGoogleApiKey,
            enabled = state.hasGoogleApiKey,
            colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f)
        ) { Text(if (state.deleteApiKeyConfirmation) "Conferma cancella" else "Cancella API key", fontSize = 13.sp) }
    }

    Text("Google Cloud TTS ha limiti gratuiti mensili. Oltre il limite puo generare costi.", color = TarloColors.Muted, fontSize = 12.sp)
    state.googleVoiceNames.chunked(2).forEach { rowItems ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            rowItems.forEach { voice ->
                val selected = voice == state.googleVoiceName
                Button(
                    onClick = { onGoogleVoiceChange(voice) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) TarloColors.Accent else TarloColors.AccentSoft,
                        contentColor = if (selected) TarloColors.PlayerText else TarloColors.Ink
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(voice, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    Text("Caratteri Google usati questo mese: ${state.googleCharsThisMonth} / ${state.googleMonthlyLimit}", color = TarloColors.Muted, fontSize = 12.sp)
    Text("Soglia sicurezza: ${state.googleSafetyLimit} caratteri", color = TarloColors.Muted, fontSize = 12.sp)
    Text("${state.googleSafetyPercent}% della quota prudenziale usata", color = TarloColors.Muted, fontSize = 12.sp)
    state.googleWarningText?.let { Text(it, color = TarloColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onTestPremiumVoice,
            enabled = state.hasGoogleApiKey && !state.googleBlocked,
            colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f)
        ) { Text("Test API key", fontSize = 13.sp) }
        Button(
            onClick = onUseAndroidForMonth,
            colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f)
        ) { Text("Usa Android questo mese", fontSize = 12.sp) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Debug quota",
            color = TarloColors.Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Questa funzione e solo per test/debug. Non usarla per aggirare limiti o generare costi inattesi.",
            color = TarloColors.Muted,
            fontSize = 11.sp
        )
        Button(
            onClick = onResetGoogleCounter,
            colors = ButtonDefaults.buttonColors(containerColor = TarloColors.SurfaceMuted, contentColor = TarloColors.Ink),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.resetCounterConfirmation) "Conferma reset contatore" else "Reset contatore solo test/debug", fontSize = 12.sp) }
    }
}

@Composable
private fun Presets(
    state: TarloUiState,
    onPresetChange: (VoicePreset) -> Unit,
    onRestorePreset: () -> Unit,
    onSavePersonalSettings: () -> Unit
) {
    Text("Preset lettura", color = TarloColors.Ink, fontWeight = FontWeight.Bold)
    VoicePreset.values().toList().chunked(2).forEach { rowItems ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            rowItems.forEach { preset ->
                val selected = preset == state.selectedPreset && !state.customSettings
                Button(
                    onClick = { onPresetChange(preset) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) TarloColors.Accent else TarloColors.AccentSoft,
                        contentColor = if (selected) TarloColors.PlayerText else TarloColors.Ink
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(preset.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onRestorePreset, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
            Text("Ripristina preset", fontSize = 13.sp)
        }
        Button(onClick = onSavePersonalSettings, colors = ButtonDefaults.buttonColors(containerColor = TarloColors.AccentSoft, contentColor = TarloColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
            Text("Salva personale", fontSize = 13.sp)
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
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = TarloColors.Accent,
                activeTrackColor = TarloColors.Accent,
                inactiveTrackColor = TarloColors.SurfaceMuted
            )
        )
    }
}

@Composable
private fun DocumentList(state: TarloUiState, onSelectDocument: (AudioDocument) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = TarloColors.Accent)
            Spacer(Modifier.width(8.dp))
            Text("Documenti", color = TarloColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        if (state.documents.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = TarloColors.Surface), shape = RoundedCornerShape(18.dp)) {
                Text("La tua lista e vuota. Importa un EPUB per creare il primo audiolibro.", color = TarloColors.Muted, modifier = Modifier.padding(18.dp))
            }
        } else {
            state.documents.forEach { document ->
                val selected = document.id == state.current?.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (selected) TarloColors.AccentSoft else TarloColors.Surface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onSelectDocument(document) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(document.title, color = TarloColors.Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(document.progressLabel, color = TarloColors.Muted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerBar(
    state: TarloUiState,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TarloColors.Player),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.current?.title ?: "Pronto per leggere", color = TarloColors.PlayerText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.current?.progressLabel ?: "Nessun EPUB", color = TarloColors.SurfaceMuted, fontSize = 12.sp)
            }
            IconButton(onClick = onPreviousPage) { Icon(Icons.Default.ArrowBack, contentDescription = "Pagina precedente", tint = TarloColors.PlayerText) }
            IconButton(onClick = onTogglePlay, modifier = Modifier.size(54.dp).clip(CircleShape).background(TarloColors.Accent)) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pausa", tint = TarloColors.PlayerText)
            }
            IconButton(onClick = onNextPage) { Icon(Icons.Default.ArrowForward, contentDescription = "Pagina successiva", tint = TarloColors.PlayerText) }
            IconButton(onClick = onStop) { Icon(Icons.Default.Stop, contentDescription = "Stop", tint = TarloColors.PlayerText) }
        }
    }
}
