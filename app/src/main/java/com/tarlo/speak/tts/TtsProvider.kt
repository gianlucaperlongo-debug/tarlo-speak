package com.tarlo.speak.tts

interface TtsProvider {
    fun speak(text: String, speed: Float, pitch: Float, utteranceId: String)
    fun stop()
    fun shutdown()
}
