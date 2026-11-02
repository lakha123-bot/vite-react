package com.example.data.repository

import com.example.data.local.ChatMessageEntity
import com.example.data.local.JarvisDao
import com.example.data.local.JarvisPreferences
import com.example.data.local.MemoryEntity
import com.example.data.model.ChatMessage
import com.example.data.model.MemoryItem
import com.example.data.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class JarvisRepository(
    private val dao: JarvisDao,
    val preferences: JarvisPreferences
) {
    val messages: Flow<List<ChatMessage>> = dao.getAllMessages().map { entities ->
        entities.map { entity ->
            ChatMessage(
                id = entity.id,
                role = when (entity.role.uppercase()) {
                    "USER" -> MessageRole.USER
                    "SYSTEM" -> MessageRole.SYSTEM
                    else -> MessageRole.JARVIS
                },
                content = entity.content,
                timestamp = entity.timestamp,
                actionExecuted = entity.actionExecuted
            )
        }
    }

    val memories: Flow<List<MemoryItem>> = dao.getAllMemories().map { entities ->
        entities.map { entity ->
            MemoryItem(
                id = entity.id,
                key = entity.factKey,
                value = entity.factValue,
                category = entity.category,
                timestamp = entity.timestamp
            )
        }
    }

    suspend fun saveMessage(message: ChatMessage): Long {
        return dao.insertMessage(
            ChatMessageEntity(
                role = message.role.name,
                content = message.content,
                timestamp = message.timestamp,
                actionExecuted = message.actionExecuted
            )
        )
    }

    suspend fun clearMessages() {
        dao.clearAllMessages()
    }

    suspend fun addMemory(key: String, value: String, category: String = "GENERAL"): Long {
        return dao.insertMemory(
            MemoryEntity(
                factKey = key.trim(),
                factValue = value.trim(),
                category = category,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMemory(id: Long) {
        dao.deleteMemory(id)
    }

    suspend fun clearMemories() {
        dao.clearAllMemories()
    }

    suspend fun seedInitialMemoryIfEmpty() {
        val current = dao.getAllMemories().first()
        if (current.isEmpty()) {
            addMemory("Creator", "LAKHA RATHOD", "SYSTEM")
            addMemory("System Identity", "LAKHA JARVIS AI - MK VII", "CORE")
            addMemory("Languages", "English, Hindi, and Hinglish fluent", "PREFERENCE")
        }

        val currentMessages = dao.getAllMessages().first()
        if (currentMessages.isEmpty()) {
            saveMessage(
                ChatMessage(
                    role = MessageRole.JARVIS,
                    content = "Systems online. All protocols operational. Greetings! I am LAKHA JARVIS AI, created by LAKHA RATHOD. How may I assist you today, Sir?",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
