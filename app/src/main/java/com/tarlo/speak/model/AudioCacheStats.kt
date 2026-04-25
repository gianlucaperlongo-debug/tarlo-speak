package com.tarlo.speak.model

data class AudioCacheStats(
    val enabled: Boolean = true,
    val fileCount: Int = 0,
    val sizeBytes: Long = 0L,
    val hitCount: Int = 0,
    val savedCharacters: Int = 0,
    val maxSizeMb: Int = 500
) {
    val sizeLabel: String
        get() {
            val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
            return if (mb < 1.0) "${(sizeBytes / 1024L).coerceAtLeast(0L)} KB" else "%.1f MB".format(mb)
        }
}
