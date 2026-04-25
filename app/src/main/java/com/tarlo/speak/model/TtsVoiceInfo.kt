package com.tarlo.speak.model

data class TtsVoiceInfo(
    val name: String,
    val languageTag: String,
    val displayLanguage: String,
    val quality: String,
    val isItalian: Boolean,
    val isEnglish: Boolean
)
