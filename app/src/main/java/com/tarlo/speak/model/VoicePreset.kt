package com.tarlo.speak.model

enum class VoicePreset(
    val label: String,
    val rate: Float,
    val pitch: Float,
    val commaPauseMs: Int,
    val periodPauseMs: Int,
    val paragraphPauseMs: Int,
    val chunkLength: Int
) {
    NATURALE("Naturale", 0.95f, 1.00f, 90, 260, 460, 760),
    AUDIOLIBRO("Audiolibro", 0.90f, 1.00f, 120, 320, 560, 680),
    STUDIO("Studio", 0.85f, 1.02f, 110, 300, 520, 620),
    LENTO_CHIARO("Lento e chiaro", 0.80f, 1.00f, 150, 380, 650, 560),
    VELOCE("Veloce", 1.15f, 1.00f, 60, 180, 320, 900);

        companion object {
            fun fromName(name: String?): VoicePreset {
                return values().firstOrNull { it.name == name } ?: AUDIOLIBRO
            }
        }
    }
