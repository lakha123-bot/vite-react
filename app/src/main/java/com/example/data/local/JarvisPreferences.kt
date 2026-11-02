package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_preferences")

class JarvisPreferences(private val context: Context) {

    companion object {
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val KEY_PERSONALITY_ID = stringPreferencesKey("personality_id")
        val KEY_LANGUAGE_MODE = stringPreferencesKey("language_mode")
        val KEY_TTS_PITCH = floatPreferencesKey("tts_pitch")
        val KEY_TTS_SPEED = floatPreferencesKey("tts_speed")
        val KEY_AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val KEY_USER_CALLSIGN = stringPreferencesKey("user_callsign")
        val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        val savedKey = prefs[KEY_GEMINI_API_KEY]?.trim()
        if (!savedKey.isNullOrBlank()) {
            savedKey
        } else {
            // Check BuildConfig fallback
            try {
                val buildKey = BuildConfig.GEMINI_API_KEY
                if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
            } catch (e: Throwable) {
                ""
            }
        }
    }

    val geminiModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GEMINI_MODEL] ?: "gemini-3.8-flash"
    }

    val personalityId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PERSONALITY_ID] ?: "jarvis"
    }

    val languageMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE_MODE] ?: "auto"
    }

    val ttsPitch: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_TTS_PITCH] ?: 1.0f
    }

    val ttsSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_TTS_SPEED] ?: 1.0f
    }

    val autoSpeak: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SPEAK] ?: true
    }

    val userCallsign: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_CALLSIGN] ?: "Sir"
    }

    val overlayEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_OVERLAY_ENABLED] ?: false
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GEMINI_API_KEY] = key.trim()
        }
    }

    suspend fun setGeminiModel(model: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GEMINI_MODEL] = model
        }
    }

    suspend fun setPersonalityId(personalityId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PERSONALITY_ID] = personalityId
        }
    }

    suspend fun setLanguageMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE_MODE] = mode
        }
    }

    suspend fun setTtsPitch(pitch: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TTS_PITCH] = pitch
        }
    }

    suspend fun setTtsSpeed(speed: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TTS_SPEED] = speed
        }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_SPEAK] = enabled
        }
    }

    suspend fun setUserCallsign(callsign: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_CALLSIGN] = callsign
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OVERLAY_ENABLED] = enabled
        }
    }
}
