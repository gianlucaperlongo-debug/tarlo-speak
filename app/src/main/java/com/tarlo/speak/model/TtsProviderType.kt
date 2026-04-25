package com.tarlo.speak.model

enum class TtsProviderType(val label: String) {
    ANDROID("Voce dispositivo Android"),
    GOOGLE_WAVENET("Google WaveNet economico"),
    GOOGLE_CHIRP("Google Chirp 3 HD premium");

    val isGoogle: Boolean
        get() = this == GOOGLE_WAVENET || this == GOOGLE_CHIRP

    companion object {
        fun fromName(name: String?): TtsProviderType {
            if (name == "GOOGLE_CLOUD") return GOOGLE_WAVENET
            return values().firstOrNull { it.name == name } ?: ANDROID
        }
    }
}
