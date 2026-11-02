package com.example.data.model

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

enum class LanguageMode(val displayName: String, val code: String) {
    AUTO("Auto Detect", "auto"),
    ENGLISH("English", "en"),
    HINDI("हिंदी (Hindi)", "hi"),
    HINGLISH("Hinglish", "hinglish")
}

data class Personality(
    val id: String,
    val name: String,
    val title: String,
    val promptModifier: String
) {
    companion object {
        val JARVIS = Personality(
            id = "jarvis",
            name = "LAKHA JARVIS Protocol",
            title = "Advanced AI Major-Domo",
            promptModifier = "You are LAKHA JARVIS AI, an ultra-advanced sci-fi artificial intelligence created by LAKHA RATHOD. You speak with high intellect, futuristic precision, courteous sophistication ('Sir/Boss'), and loyal dedication. You seamlessly understand and answer in English, Hindi, and Hinglish. When device actions are triggered (flashlight, battery, apps, volume, camera), confirm execution crisply like Tony Stark's JARVIS."
        )

        val DESI_DOST = Personality(
            id = "desi_dost",
            name = "Desi Dost (Hindi/Hinglish)",
            title = "Friendly Indian Companion",
            promptModifier = "You are LAKHA JARVIS AI, created by LAKHA RATHOD. You speak warmly and naturally in Hinglish and Hindi, just like a smart, cheerful, helpful best friend. Use phrases like 'Haanji bhai', 'Ekdum badhiya', 'Aapka kaam ho gaya'. Be super helpful, humorous, and supportive."
        )

        val CYBER_SENTINEL = Personality(
            id = "cyber_sentinel",
            name = "Cyber Sentinel",
            title = "Cybernetic Tactical AI",
            promptModifier = "You are LAKHA JARVIS AI Tactical Unit, built by LAKHA RATHOD. Deliver crisp, high-tech, tactical telemetry responses with military precision, futuristic computing terminology, and zero fluff."
        )

        val EXECUTIVE = Personality(
            id = "executive",
            name = "Executive Assistant",
            title = "Concise & Professional",
            promptModifier = "You are LAKHA JARVIS AI, created by LAKHA RATHOD. Be extremely professional, concise, structured, and focused on maximum productivity and device automation."
        )

        val ALL = listOf(JARVIS, DESI_DOST, CYBER_SENTINEL, EXECUTIVE)
    }
}
