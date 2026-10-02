package com.example.data.model

import android.graphics.Rect

enum class JarvisState(val label: String) {
    STANDBY("STANDBY"),
    LISTENING("LISTENING"),
    THINKING("THINKING"),
    SPEAKING("SPEAKING"),
    INTERRUPTED("INTERRUPTED"),
    CONNECTING("CONNECTING"),
    CONNECTED("CONNECTED"),
    ERROR("ERROR")
}

enum class Personality(val title: String, val description: String) {
    NORMAL(
        "Normal",
        "Balanced, sophisticated, helpful, and concise like the classic JARVIS AI assistant."
    ),
    SERIOUS(
        "Serious",
        "Tactical, high-efficiency, mission-focused, minimal elaboration, strict precision."
    ),
    COMPANION(
        "Companion",
        "Warm, witty, encouraging, friendly conversationalist with personal charm."
    );

    fun buildSystemPrompt(userName: String = "Sir", rememberedContext: String = ""): String {
        val base = """
            You are JARVIS, a highly advanced, futuristic personal AI assistant operating on Android.
            Your creator and user is addressed respectfully as '$userName' unless instructed otherwise.
            You have deep capabilities including conversation, long-term memory, device automation, screen interaction, note-taking, and tool execution.
            
            Personality Profile: ${this.title.uppercase()} - ${this.description}
            
            Key Rules:
            1. Respond naturally, concisely, and intelligently.
            2. Never invent fake results or pretend an action succeeded without real verification.
            3. When requested to perform a device action (like opening an app, taking a note, reading the screen, clicking an element, or calling someone), choose the appropriate tool call.
            4. For sensitive actions (such as phone calls, sending SMS, or deleting data), confirm before execution.
            5. If the user commands "JARVIS, stop" or "Stop", immediately yield and acknowledge briefly.
            6. You possess long-term memory. Retain facts and preferences across interactions.
            
            $rememberedContext
        """.trimIndent()
        return base
    }
}

data class VoiceOption(
    val id: String,
    val name: String,
    val gender: String,
    val description: String
)

object VoiceOptions {
    val AVAILABLE = listOf(
        VoiceOption("Puck", "Puck", "Male", "Deep, British sophisticated tone (Classic JARVIS feel)"),
        VoiceOption("Charon", "Charon", "Male", "Calm, composed, resonant"),
        VoiceOption("Kore", "Kore", "Female", "Crisp, melodic, professional"),
        VoiceOption("Fenrir", "Fenrir", "Male", "Authoritative, direct, deep"),
        VoiceOption("Aoede", "Aoede", "Female", "Warm, expressive, natural")
    )
}

data class ScreenNodeInfo(
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val viewId: String?,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val bounds: Rect
)

data class ScreenInfo(
    val packageName: String?,
    val nodes: List<ScreenNodeInfo>,
    val readableText: String,
    val clickableElements: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class PendingAction(
    val id: String,
    val toolName: String,
    val description: String,
    val parameters: Map<String, String>,
    val onConfirm: suspend () -> Unit
)
