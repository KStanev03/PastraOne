package com.example.pastraone

data class PlayResult(
    val success: Boolean,
    val message: String,
    val captured: Boolean = false,
    val isBastra: Boolean = false,
    val bastraPoints: Int = 0,
    val capturedCards: List<Card> = emptyList()
)
