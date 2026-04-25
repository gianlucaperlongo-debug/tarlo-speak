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
    fun loadEpub(uri: Uri, maxChunkLength: Int = 680): AudioDocument {
        val htmlParts = mutableListOf<String>()
        var metadataTitle: String? = null

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase(Locale.ROOT)
                    val readableEntry = !entry.isDirectory &&
                        (name.endsWith(".opf") || name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm"))

                    if (readableEntry) {
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

        val allText = cleanReadableText(htmlParts.joinToString(separator = "\n\n") { stripHtml(it) })
        val title = metadataTitle?.takeIf { it.isNotBlank() } ?: fallbackTitle(uri)

        return AudioDocument(
            id = UUID.randomUUID().toString(),
            title = title,
            chunks = chunkText(allText, maxChunkLength)
        )
    }

    fun stripHtml(html: String): String {
        return html
            .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("</(p|div|section|article|chapter|h1|h2|h3|h4|li)>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#8217;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&#160;", " ")
            .trim()
    }

    fun cleanReadableText(text: String): String {
        return text
            .replace('\u00A0', ' ')
            .replace('\u200B', ' ')
            .replace('\u200C', ' ')
            .replace('\u200D', ' ')
            .replace('\uFEFF', ' ')
            .replace("\u2018", "'")
            .replace("\u2019", "'")
            .replace("\u201C", "\"")
            .replace("\u201D", "\"")
            .replace("\u00AB", "\"")
            .replace("\u00BB", "\"")
            .replace("\u00E2\u20AC\u02DC", "'")
            .replace("\u00E2\u20AC\u2122", "'")
            .replace("\u00E2\u20AC\u0153", "\"")
            .replace("\u00E2\u20AC\u009D", "\"")
            .replace("\u00C2\u00AB", "\"")
            .replace("\u00C2\u00BB", "\"")
            .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
            .replace(Regex("\\n\\s*\\n\\s*\\n+"), "\n\n")
            .lines()
            .map { it.trim() }
            .filterNot { it.matches(Regex("\\d{1,4}")) }
            .joinToString("\n")
            .replace(Regex("([,;:])([^\\s\\n])"), "$1 $2")
            .replace(Regex("([.!?])([^\\s\\n\"'])"), "$1 $2")
            .replace(Regex(" {2,}"), " ")
            .trim()
    }

    fun chunkText(text: String, maxChunkLength: Int = 680): List<String> {
        if (text.isBlank()) return emptyList()

        val result = mutableListOf<String>()
        val current = StringBuilder()
        val normalizedMax = maxChunkLength.coerceIn(320, 1100)

        text.split(Regex("\\n\\s*\\n+")).forEach { paragraph ->
            val sentences = paragraph.split(Regex("(?<=[.!?;:])\\s+"))
            sentences.forEach { sentence ->
                val clean = sentence.trim()
                if (clean.isBlank()) return@forEach

                if (current.length + clean.length + 1 > normalizedMax && current.isNotBlank()) {
                    result += current.toString().trim()
                    current.clear()
                }

                if (clean.length > normalizedMax) {
                    clean.chunked(normalizedMax - 40).forEach { result += it.trim() }
                } else {
                    current.append(clean).append(' ')
                }
            }

            if (current.isNotBlank()) {
                result += current.toString().trim()
                current.clear()
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
