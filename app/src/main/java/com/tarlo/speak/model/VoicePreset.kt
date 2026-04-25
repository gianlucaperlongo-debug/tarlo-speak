package com.tarlo.speak.model

enum class VoicePreset(
    val label: String,
    val rate: Float,
    val pitch: Float,
    val commaPauseMs: Int,
    val periodPauseMs: Int,
    val paragraphPauseMs: Int,
    val pagePauseMs: Int,
    val fontSizeSp: Float,
    val lineHeightMultiplier: Float,
    val pageLengthBase: Int
) {
    AUDIOBOOK_PRO("Audiolibro PRO", 0.90f, 1.00f, 140, 320, 650, 850, 20f, 1.45f, 850),
    NATURALE("Naturale", 0.95f, 1.00f, 110, 260, 520, 700, 19f, 1.40f, 960),
    STUDIO("Studio", 0.85f, 1.02f, 170, 380, 750, 950, 21f, 1.55f, 760),
    LENTO_CHIARO("Lento e chiaro", 0.78f, 1.00f, 200, 480, 900, 1100, 22f, 1.60f, 690),
    VELOCE("Veloce", 1.15f, 1.00f, 70, 180, 360, 500, 18f, 1.32f, 1080),
    FOCUS("Focus", 1.00f, 0.98f, 90, 230, 480, 650, 20f, 1.42f, 880);

    companion object {
        fun fromName(name: String?): VoicePreset {
            return values().firstOrNull { it.name == name } ?: AUDIOBOOK_PRO
        }
    }
}
