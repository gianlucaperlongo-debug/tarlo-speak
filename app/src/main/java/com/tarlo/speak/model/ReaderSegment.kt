package com.tarlo.speak.model

data class ReaderSegment(
    val text: String,
    val start: Int,
    val end: Int,
    val paragraphEnd: Boolean = false
)
