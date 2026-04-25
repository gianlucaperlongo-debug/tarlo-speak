package com.tarlo.speak.model

data class AudioDocument(
    val id: String,
    val title: String,
    val chunks: List<String>,
    val index: Int = 0,
    val speed: Float = 1.0f,
    val pitch: Float = 0.92f
) {
    val progressLabel: String
        get() = if (chunks.isEmpty()) "0/0 blocchi" else "${index + 1}/${chunks.size} blocchi"

    val currentChunk: String
        get() = chunks.getOrNull(index).orEmpty()
}
