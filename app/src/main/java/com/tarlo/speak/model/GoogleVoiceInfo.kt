package com.tarlo.speak.model

data class GoogleVoiceInfo(
    val name: String,
    val languageCodes: List<String>,
    val ssmlGender: String = "Non specificato",
    val naturalSampleRateHertz: Int = 0
) {
    val displayLanguage: String
        get() = languageCodes.joinToString(", ")

    val sampleRateLabel: String
        get() = if (naturalSampleRateHertz > 0) "${naturalSampleRateHertz} Hz" else "sample rate n/d"

    val isItalian: Boolean
        get() = languageCodes.any { it.equals("it-IT", ignoreCase = true) }

    val isChirp: Boolean
        get() {
            val lower = name.lowercase()
            return "chirp3-hd" in lower || "chirp" in lower || "hd" in lower
        }

    val isWavenet: Boolean
        get() = "wavenet" in name.lowercase()
}
