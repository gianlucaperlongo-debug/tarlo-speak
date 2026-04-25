package com.tarlo.speak.model

data class TextHighlight(
    val start: Int = 0,
    val end: Int = 0,
    val exact: Boolean = false
) {
    val isActive: Boolean
        get() = end > start
}
