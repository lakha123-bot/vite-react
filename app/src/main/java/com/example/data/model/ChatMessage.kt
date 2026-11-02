package com.example.data.model

enum class MessageRole {
    USER,
    JARVIS,
    SYSTEM
}

data class ChatMessage(
    val id: Long = 0,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionExecuted: String? = null,
    val isStreaming: Boolean = false
)
