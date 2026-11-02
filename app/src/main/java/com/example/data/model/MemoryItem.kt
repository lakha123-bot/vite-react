package com.example.data.model

data class MemoryItem(
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "GENERAL",
    val timestamp: Long = System.currentTimeMillis()
)
