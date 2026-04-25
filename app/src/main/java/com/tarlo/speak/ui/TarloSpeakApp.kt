package com.tarlo.speak.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarlo.speak.model.AudioDocument
import com.tarlo.speak.model.TextHighlight
import com.tarlo.speak.model.TtsVoiceInfo

data class TarloUiState(
    val documents: List<AudioDocument> = emptyList(),
    val current: AudioDocument? = null,
    val isPlaying: Boolean = false,
    val statusText: String = "Importa un EPUB per iniziare",
    val ttsReady: Boolean = false,
    val voices: List<TtsVoiceInfo> = emptyList(),
    val selectedVoiceName: String? = null,
    val speechRate: Float = 1.0f,
    val voicePitch: Float = 0.92f,
    val highlight: TextHighlight = TextHighlight()
)

@Composable
fun TarloSpeakApp(
    state: TarloUiState,
    onImportClick: () -> Unit,
    onSelectDocument: (AudioDocument) -> Unit,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onSkip: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onVoiceChange: (String) -> Unit,
    onTestVoice: () -> Unit
) {
    MaterialTheme {
        Surface(color = TarloColors.Background, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 132.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item { HomeHeader(state, onImportClick) }
                    item { CurrentReaderCard(state) }
                    item { VoiceSettings(state, onSpeedChange, onPitchChange, onVoiceChange, onTestVoice) }
                    item { DocumentList(state, onSelectDocument) }
                }

                PlayerBar(
                    state = state,
                    onTogglePlay = onTogglePlay,
                    onStop = onStop,
                    onSkip = onSkip,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(state: TarloUiState, onImportClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TarloColors.Accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TarloColors.PlayerText)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Tarlo Speak", color = TarloColors.Ink, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text("Leggi EPUB con voce naturale", color = TarloColors.Muted, fontSize = 15.sp)
            }
        }

        Button(
            onClick = onImportClick,
            colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Importa EPUB", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Text(
            text = state.statusText,
            color = if (state.ttsReady) TarloColors.Accent else TarloColors.Muted,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun CurrentReaderCard(state: TarloUiState) {
    val current = state.current
    Card(
        colors = CardDefaults.cardColors(containerColor = TarloColors.Surface),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Reader", color = TarloColors.Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                current?.title ?: "Nessun documento selezionato",
                color = TarloColors.Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                current?.progressLabel ?: "Importa un libro dalla tua libreria EPUB.",
                color = TarloColors.Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            HighlightedReaderText(
                text = current?.currentChunk?.takeIf { it.isNotBlank() } ?: "Qui apparira il testo attualmente in lettura.",
                highlight = state.highlight
            )
        }
    }
}

@Composable
private fun HighlightedReaderText(text: String, highlight: TextHighlight) {
    val safeStart = highlight.start.coerceIn(0, text.length)
    val safeEnd = highlight.end.coerceIn(safeStart, text.length)
    val annotated = buildAnnotatedString {
        append(text.substring(0, safeStart))
        if (safeEnd > safeStart) {
            withStyle(
                SpanStyle(
                    color = TarloColors.Ink,
                    background = if (highlight.exact) TarloColors.Highlight else TarloColors.HighlightSoft,
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
        fontSize = 18.sp,
        lineHeight = 27.sp
    )
}

@Composable
private fun VoiceSettings(
    state: TarloUiState,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onVoiceChange: (String) -> Unit,
    onTestVoice: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TarloColors.Surface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Voce", color = TarloColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Scegli una voce installata nel motore Text-to-Speech Android.", color = TarloColors.Muted, fontSize = 14.sp)

            if (state.voices.isEmpty()) {
                Text("Installa altre voci TTS dalle impostazioni Android", color = TarloColors.Muted, fontSize = 14.sp)
            } else {
                LazyColumn(
                    modifier = Modifier.height(if (state.voices.size == 1) 92.dp else 190.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.voices) { voice ->
                        VoiceRow(
                            voice = voice,
                            selected = voice.name == state.selectedVoiceName,
                            onClick = { onVoiceChange(voice.name) }
                        )
                    }
                }

                if (state.voices.size == 1) {
                    Text(
                        "Puoi installare altre voci dalle impostazioni di sintesi vocale Android",
                        color = TarloColors.Muted,
                        fontSize = 13.sp
                    )
                }
            }

            Button(
                onClick = onTestVoice,
                enabled = state.ttsReady,
                colors = ButtonDefaults.buttonColors(containerColor = TarloColors.Accent, contentColor = TarloColors.PlayerText),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Test voce", fontWeight = FontWeight.Bold)
            }

            Text("Velocita ${"%.2f".format(state.speechRate)}x", color = TarloColors.Muted)
            Slider(
                value = state.speechRate,
                onValueChange = onSpeedChange,
                valueRange = 0.65f..1.70f,
                colors = SliderDefaults.colors(
                    thumbColor = TarloColors.Accent,
                    activeTrackColor = TarloColors.Accent,
                    inactiveTrackColor = TarloColors.SurfaceMuted
                )
            )

            Text("Tono ${"%.2f".format(state.voicePitch)}", color = TarloColors.Muted)
            Slider(
                value = state.voicePitch,
                onValueChange = onPitchChange,
                valueRange = 0.75f..1.20f,
                colors = SliderDefaults.colors(
                    thumbColor = TarloColors.Accent,
                    activeTrackColor = TarloColors.Accent,
                    inactiveTrackColor = TarloColors.SurfaceMuted
                )
            )
        }
    }
}

@Composable
private fun VoiceRow(voice: TtsVoiceInfo, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) TarloColors.Accent else TarloColors.AccentSoft,
            contentColor = if (selected) TarloColors.PlayerText else TarloColors.Ink
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(voice.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${voice.displayLanguage} (${voice.languageTag}) - ${voice.quality}",
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) TarloColors.PlayerText else TarloColors.Muted
            )
        }
    }
}

@Composable
private fun DocumentList(state: TarloUiState, onSelectDocument: (AudioDocument) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = TarloColors.Accent)
            Spacer(Modifier.width(8.dp))
            Text("Documenti", color = TarloColors.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        if (state.documents.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TarloColors.Surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "La tua lista e vuota. Importa un EPUB per creare il primo audiolibro.",
                    color = TarloColors.Muted,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            state.documents.forEach { document ->
                val selected = document.id == state.current?.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (selected) TarloColors.AccentSoft else TarloColors.Surface),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDocument(document) }
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(TarloColors.Accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("T", color = TarloColors.PlayerText, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(document.title, color = TarloColors.Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(document.progressLabel, color = TarloColors.Muted, fontSize = 13.sp)
                        }
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
    onSkip: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TarloColors.Player),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.current?.title ?: "Pronto per leggere", color = TarloColors.PlayerText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.current?.progressLabel ?: "Nessun EPUB", color = TarloColors.SurfaceMuted, fontSize = 12.sp)
            }
            IconButton(onClick = { onSkip(-3) }) {
                Icon(Icons.Default.FastRewind, contentDescription = "Indietro", tint = TarloColors.PlayerText)
            }
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(TarloColors.Accent)
            ) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pausa", tint = TarloColors.PlayerText)
            }
            IconButton(onClick = { onSkip(3) }) {
                Icon(Icons.Default.FastForward, contentDescription = "Avanti", tint = TarloColors.PlayerText)
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = TarloColors.PlayerText)
            }
        }
    }
}
