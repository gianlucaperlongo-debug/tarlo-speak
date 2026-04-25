package com.tarlo.speak.model

enum class HighlightMode(val label: String) {
    WORD("Parola"),
    SENTENCE("Frase"),
    PARAGRAPH("Paragrafo");

    companion object {
        fun fromName(name: String?): HighlightMode {
            return values().firstOrNull { it.name == name } ?: SENTENCE
        }
    }
}
