package com.tarlo.speak.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tarlo.speak.model.AudioDocument
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

class DocumentRepository(private val context: Context) {
    fun loadEpub(uri: Uri): AudioDocument {
        val htmlParts = mutableListOf<String>()
        var metadataTitle: String? = null

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase(Locale.ROOT)
                    if (!entry.isDirectory && (name.endsWith(".opf") || name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm"))) {
                        val bytes = ByteArrayOutputStream()
                        zip.copyTo(bytes)
                        val text = bytes.toString(Charsets.UTF_8.name())

                        if (name.endsWith(".opf") && metadataTitle.isNullOrBlank()) {
                            metadataTitle = readEpubTitle(text)
                        }

                        if (name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm")) {
                            htmlParts += text
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        val allText = htmlParts.joinToString(separator = "\n") { stripHtml(it) }
        val title = metadataTitle?.takeIf { it.isNotBlank() } ?: fallbackTitle(uri)

        return AudioDocument(
            id = UUID.randomUUID().toString(),
            title = title,
            chunks = chunkText(allText)
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
        val current = StringBuilder()

        sentences.forEach { sentence ->
            val clean = sentence.trim()
            if (clean.isBlank()) return@forEach

            if (current.length + clean.length + 1 > 900 && current.isNotBlank()) {
                result += current.toString().trim()
                current.clear()
            }

            if (clean.length > 900) {
                clean.chunked(850).forEach { result += it.trim() }
            } else {
                current.append(clean).append(' ')
            }
        }

        if (current.isNotBlank()) result += current.toString().trim()
        return result
    }

    fun safeName(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').ifBlank { "libro" }
    }

    private fun readEpubTitle(opf: String): String? {
        val match = Regex("<dc:title[^>]*>([\\s\\S]*?)</dc:title>", RegexOption.IGNORE_CASE).find(opf)
            ?: Regex("<title[^>]*>([\\s\\S]*?)</title>", RegexOption.IGNORE_CASE).find(opf)

        return match
            ?.groupValues
            ?.getOrNull(1)
            ?.let { stripHtml(it) }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun fallbackTitle(uri: Uri): String {
        val displayName = queryDisplayName(uri)
        val raw = displayName ?: uri.lastPathSegment ?: "Libro EPUB"
        return raw
            .substringAfterLast('/')
            .substringAfterLast(':')
            .removeSuffix(".epub")
            .removeSuffix(".EPUB")
            .trim()
            .ifBlank { "Libro EPUB" }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
    }
}
