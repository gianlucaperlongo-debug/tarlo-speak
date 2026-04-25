package com.tarlo.speak.model

enum class TtsProviderType(val label: String) {
    ANDROID("Voce dispositivo Android"),
    GOOGLE_CLOUD("Google Cloud WaveNet");

    companion object {
        fun fromName(name: String?): TtsProviderType {
            return values().firstOrNull { it.name == name } ?: ANDROID
        }
    }
}
