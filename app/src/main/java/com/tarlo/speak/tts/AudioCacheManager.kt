package com.tarlo.speak.tts

import android.content.Context
import com.tarlo.speak.model.AudioCacheStats
import com.tarlo.speak.model.TtsProviderType
import java.io.File
import java.security.MessageDigest
import java.util.Locale

class AudioCacheManager(context: Context) {
    private val cacheDirectory = File(context.applicationContext.cacheDir, "tarlo_audio_cache")

    init {
        cacheDirectory.mkdirs()
    }

    fun cacheFile(
        provider: TtsProviderType,
        voiceName: String,
        language: String,
        text: String,
        speed: Float,
        pitch: Float,
        presetName: String,
        chirpUsesPlayerSpeed: Boolean
    ): File {
        val normalized = normalizeForCache(text)
        val audioSettings = if (provider == TtsProviderType.GOOGLE_CHIRP && chirpUsesPlayerSpeed) {
            "playerSpeed=${String.format(Locale.US, "%.2f", speed)}|pitch=default"
        } else {
            "rate=${String.format(Locale.US, "%.2f", speed)}|pitch=${String.format(Locale.US, "%.2f", pitch)}"
        }
        val key = listOf(
            "v2",
            provider.name,
            voiceName,
            language,
            presetName,
            audioSettings,
            normalized
        ).joinToString("|")
        return File(cacheDirectory, sha256(key) + ".mp3")
    }

    fun stats(enabled: Boolean, hitCount: Int, savedCharacters: Int, maxSizeMb: Int): AudioCacheStats {
        val files = audioFiles()
        return AudioCacheStats(
            enabled = enabled,
            fileCount = files.size,
            sizeBytes = files.sumOf { it.length() },
            hitCount = hitCount,
            savedCharacters = savedCharacters,
            maxSizeMb = maxSizeMb
        )
    }

    fun clear(): Int {
        val files = audioFiles()
        files.forEach { runCatching { it.delete() } }
        return files.size
    }

    fun trimToLimit(maxSizeMb: Int) {
        val maxBytes = maxSizeMb.toLong() * 1024L * 1024L
        if (maxBytes <= 0L) return
        var files = audioFiles()
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return

        files = files.sortedBy { it.lastModified() }.toMutableList()
        for (file in files) {
            if (total <= maxBytes) break
            val length = file.length()
            if (file.delete()) total -= length
        }
    }

    fun hasEnoughRoomFor(bytesToWrite: Int, maxSizeMb: Int): Boolean {
        val usable = cacheDirectory.usableSpace
        val maxBytes = maxSizeMb.toLong() * 1024L * 1024L
        val current = audioFiles().sumOf { it.length() }
        return usable > bytesToWrite + MIN_FREE_BYTES && current + bytesToWrite <= maxBytes + bytesToWrite
    }

    private fun audioFiles(): List<File> {
        return cacheDirectory.listFiles { file -> file.isFile && file.extension.equals("mp3", ignoreCase = true) }
            ?.toList()
            .orEmpty()
    }

    private fun normalizeForCache(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val MIN_FREE_BYTES = 20L * 1024L * 1024L
    }
}
