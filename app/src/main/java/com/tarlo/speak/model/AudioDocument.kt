package com.tarlo.speak.model

data class AudioDocument(
    val id: String,
    val title: String,
    val pages: List<String>,
    val pageIndex: Int = 0,
    val segmentIndex: Int = 0,
    val speed: Float = 0.90f,
    val pitch: Float = 1.0f,
    val providerType: TtsProviderType = TtsProviderType.ANDROID,
    val presetName: String = VoicePreset.AUDIOBOOK_PRO.name
) {
    val progressLabel: String
        get() = if (pages.isEmpty()) "Pagina 0 di 0" else "Pagina ${pageIndex + 1} di ${pages.size}"

    val progressPercent: Int
        get() = if (pages.isEmpty()) 0 else (((pageIndex + 1).toFloat() / pages.size.toFloat()) * 100).toInt().coerceIn(0, 100)

    val currentPage: String
        get() = pages.getOrNull(pageIndex).orEmpty()
}
