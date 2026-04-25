package it.iltarlo.tarlospeaker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

data class AudioBook(
    val id: String,
    val title: String,
    val chunks: List<String>,
    val index: Int = 0,
    val speed: Float = 1.0f,
    val pitch: Float = 0.92f
)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady by mutableStateOf(false)
    private var sleepTimer: CountDownTimer? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    var books by mutableStateOf(listOf<AudioBook>())
    var current by mutableStateOf<AudioBook?>(null)
    var isPlaying by mutableStateOf(false)
    var statusText by mutableStateOf("Carica un EPUB per iniziare")
    var isExporting by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    val book = current ?: return@runOnUiThread
                    if (!isPlaying) return@runOnUiThread
                    val nextIndex = book.index + 1
                    if (nextIndex < book.chunks.size) {
                        updateCurrent(book.copy(index = nextIndex))
                        speakCurrent()
                    } else {
                        isPlaying = false
                        statusText = "Fine libro"
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                runOnUiThread {
                    isPlaying = false
                    statusText = "Errore durante la lettura"
                }
            }
        })

        setContent {
            TarloSpeakerUi()
        }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts?.language = Locale.ITALIAN
            statusText = "TTS italiano pronto"
        } else {
            statusText = "Text-to-Speech non disponibile"
        }
    }

    override fun onDestroy() {
        sleepTimer?.cancel()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    fun openEpubPicker() {
        statusText = "Seleziona un file EPUB"
    }

    fun togglePlay() {
        if (!ttsReady) {
            statusText = "Text-to-Speech non ancora pronto"
            return
        }
        val book = current
        if (book == null) {
            statusText = "Nessun libro selezionato"
            return
        }
        if (isPlaying) {
            isPlaying = false
            tts?.stop()
            statusText = "Pausa"
        } else {
            isPlaying = true
            statusText = "Lettura in corso"
            speakCurrent()
        }
    }

    fun speakCurrent() {
        val book = current ?: return
        val chunk = book.chunks.getOrNull(book.index) ?: return
        tts?.setSpeechRate(book.speed)
        tts?.setPitch(book.pitch)
        tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, "chunk-${book.id}-${book.index}")
    }

    fun skip(delta: Int) {
        val book = current ?: return
        val newIndex = (book.index + delta).coerceIn(0, (book.chunks.size - 1).coerceAtLeast(0))
        updateCurrent(book.copy(index = newIndex))
        statusText = "Blocco ${newIndex + 1}/${book.chunks.size}"
        if (isPlaying) speakCurrent()
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimer?.cancel()
        sleepTimer = object : CountDownTimer(minutes * 60_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                val left = (millisUntilFinished / 60_000L) + 1
                statusText = "Timer notte: $left min"
            }

            override fun onFinish() {
                isPlaying = false
                tts?.stop()
                statusText = "Timer notte concluso"
            }
        }.start()
    }

    fun exportCurrentAsWavChunks() {
        val book = current ?: run {
            statusText = "Nessun libro da esportare"
            return
        }
        if (!ttsReady) {
            statusText = "TTS non pronto"
            return
        }
        isExporting = true
        statusText = "Export WAV in corso"
        scope.launch {
            withContext(Dispatchers.IO) {
                val base = File(getExternalFilesDir(null), "TarloSpeakerExports/${safeName(book.title)}")
                base.mkdirs()
                book.chunks.take(50).forEachIndexed { i, text ->
                    val out = File(base, "chunk_${i + 1}.wav")
                    val params = Bundle().apply {
                        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "export-${book.id}-$i")
                    }
                    tts?.setSpeechRate(book.speed)
                    tts?.setPitch(book.pitch)
                    tts?.synthesizeToFile(text, params, out, "export-${book.id}-$i")
                    Thread.sleep(180)
                }
            }
            isExporting = false
            statusText = "Export completato"
        }
    }

    fun loadEpub(uri: Uri): AudioBook {
        val chunks = mutableListOf<String>()
        val title = queryTitle(uri)
        contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase(Locale.ROOT)
                    if (!entry.isDirectory && (name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm"))) {
                        val buffer = ByteArrayOutputStream()
                        zip.copyTo(buffer)
                        val html = buffer.toString(Charsets.UTF_8.name())
                        chunks += chunkText(stripHtml(html))
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return AudioBook(
            id = UUID.randomUUID().toString(),
            title = title,
            chunks = chunks.filter { it.isNotBlank() }
        )
    }

    fun stripHtml(html: String): String {
        return html
            .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun chunkText(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        val sentences = text.split(Regex("(?<=[.!?;:])\\s+"))
        val currentBlock = StringBuilder()
        sentences.forEach { sentence ->
            val clean = sentence.trim()
            if (clean.isBlank()) return@forEach
            if (currentBlock.length + clean.length + 1 > 900) {
                if (currentBlock.isNotBlank()) {
                    result += currentBlock.toString().trim()
                    currentBlock.clear()
                }
            }
            if (clean.length > 900) {
                clean.chunked(850).forEach { result += it.trim() }
            } else {
                currentBlock.append(clean).append(' ')
            }
        }
        if (currentBlock.isNotBlank()) result += currentBlock.toString().trim()
        return result
    }

    fun safeName(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').ifBlank { "libro" }
    }

    private fun queryTitle(uri: Uri): String {
        val last = uri.lastPathSegment ?: "Libro EPUB"
        return last.substringAfterLast('/').removeSuffix(".epub").ifBlank { "Libro EPUB" }
    }

    private fun updateCurrent(book: AudioBook) {
        current = book
        books = books.map { if (it.id == book.id) book else it }
    }

    private fun updateSpeed(value: Float) {
        current?.let { updateCurrent(it.copy(speed = value)) }
    }

    private fun updatePitch(value: Float) {
        current?.let { updateCurrent(it.copy(pitch = value)) }
    }

    @Composable
    fun TarloSpeakerUi() {
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@rememberLauncherForActivityResult
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                scope.launch {
                    statusText = "Caricamento EPUB..."
                    val book = withContext(Dispatchers.IO) { loadEpub(uri) }
                    books = books + book
                    current = book
                    statusText = "Libro caricato: ${book.chunks.size} blocchi"
                }
            }
        }

        MaterialTheme {
            Surface(color = Black, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 132.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        item { Header() }
                        item {
                            ActionButtons(
                                onOpen = {
                                    openEpubPicker()
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/epub+zip"
                                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/epub+zip", "application/octet-stream"))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                                    }
                                    picker.launch(intent)
                                },
                                onExport = { exportCurrentAsWavChunks() }
                            )
                        }
                        item { CurrentBookCard() }
                        item { VoiceControls() }
                        item { SleepTimerSection() }
                        item { LibrarySection() }
                    }
                    PlayerBar(Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }

    @Composable
    fun Header() {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Black)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Tarlo Speaker", color = White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("EPUB in audiolibro", color = Muted, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(statusText, color = SpotifyGreen, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }

    @Composable
    fun ActionButtons(onOpen: () -> Unit, onExport: () -> Unit) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onOpen,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Black),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Carica EPUB", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = CardBg, contentColor = White),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (isExporting) CircularProgressIndicator(color = SpotifyGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.IosShare, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export WAV", fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun CurrentBookCard() {
        val book = current
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(book?.title ?: "Nessun libro selezionato", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    book?.let { "Blocco ${it.index + 1}/${it.chunks.size}" } ?: "Carica un EPUB per vedere il testo",
                    color = SpotifyGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    book?.chunks?.getOrNull(book.index) ?: "Il blocco attualmente in lettura apparira qui.",
                    color = White,
                    fontSize = 17.sp,
                    lineHeight = 25.sp
                )
            }
        }
    }

    @Composable
    fun VoiceControls() {
        val book = current ?: AudioBook("", "", emptyList())
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Controlli voce", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Text("Velocita ${"%.2f".format(book.speed)}x", color = Muted)
                Slider(
                    value = book.speed,
                    onValueChange = { updateSpeed(it) },
                    valueRange = 0.65f..1.70f,
                    colors = sliderColors()
                )
                Text("Pitch ${"%.2f".format(book.pitch)}", color = Muted)
                Slider(
                    value = book.pitch,
                    onValueChange = { updatePitch(it) },
                    valueRange = 0.75f..1.20f,
                    colors = sliderColors()
                )
            }
        }
    }

    @Composable
    fun SleepTimerSection() {
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = SpotifyGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Timer notte", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(10, 20, 30).forEach { minutes ->
                        Button(
                            onClick = { setSleepTimer(minutes) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkChip, contentColor = White),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text("$minutes min")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun LibrarySection() {
        Column {
            Text("Libreria sessione", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            if (books.isEmpty()) {
                Text("Nessun EPUB caricato in questa sessione.", color = Muted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    books.forEach { book ->
                        val selected = book.id == current?.id
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (selected) SelectedCard else CardBg),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    current = book
                                    statusText = "Libro selezionato"
                                }
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SpotifyGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("T", color = Black, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(book.title, color = White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${book.index + 1}/${book.chunks.size} blocchi", color = Muted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PlayerBar(modifier: Modifier = Modifier) {
        val book = current
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(book?.title ?: "Tarlo Speaker", color = White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(book?.let { "${it.index + 1}/${it.chunks.size}" } ?: "Pronto", color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = { skip(-3) }) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Indietro di 3 blocchi", tint = White)
                }
                IconButton(
                    onClick = { togglePlay() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreen)
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pausa", tint = Black)
                }
                IconButton(onClick = { skip(3) }) {
                    Icon(Icons.Default.FastForward, contentDescription = "Avanti di 3 blocchi", tint = White)
                }
            }
        }
    }

    @Composable
    private fun sliderColors() = SliderDefaults.colors(
        thumbColor = SpotifyGreen,
        activeTrackColor = SpotifyGreen,
        inactiveTrackColor = DarkChip
    )

    companion object {
        private val Black = Color(0xFF000000)
        private val White = Color(0xFFFFFFFF)
        private val SpotifyGreen = Color(0xFF1DB954)
        private val CardBg = Color(0xFF121212)
        private val SelectedCard = Color(0xFF1F2A22)
        private val DarkChip = Color(0xFF2A2A2A)
        private val Muted = Color(0xFFB3B3B3)
    }
}
