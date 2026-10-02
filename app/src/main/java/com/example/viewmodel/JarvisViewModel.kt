package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessageEntity
import com.example.data.local.JarvisDatabase
import com.example.data.local.JarvisRepository
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.local.PreferencesManager
import com.example.data.model.JarvisState
import com.example.data.model.PendingAction
import com.example.network.GeminiLiveWebSocketClient
import com.example.network.GeminiRestClient
import com.example.network.GeminiResult
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisAutomationEngine
import com.example.voice.JarvisVoiceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

data class JarvisUiState(
    val currentScreen: String = "MAIN", // "MAIN", "CALLING", "CHAT", "NOTES_MEMORY", "AUTOMATION", "SETTINGS"
    val jarvisState: JarvisState = JarvisState.STANDBY,
    val audioAmplitude: Float = 0.1f,
    val liveTranscript: String = "",
    val latestAiResponse: String = "JARVIS systems initialized. Standing by for voice or text directives, Sir.",
    val pendingAction: PendingAction? = null,
    val isMicMuted: Boolean = false,
    val isSpeakerActive: Boolean = true,
    val isLiveWsConnected: Boolean = false,
    val isAccessibilityActive: Boolean = false,
    val hasApiKey: Boolean = false,
    val statusMessage: String = "ONLINE • READY",
    val isPttPressed: Boolean = false,
    val triggerMode: String = "BOTH"
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = JarvisDatabase.getInstance(application)
    val repository = JarvisRepository(
        noteDao = database.noteDao(),
        memoryDao = database.memoryDao(),
        chatMessageDao = database.chatMessageDao()
    )

    val preferencesManager = PreferencesManager(application)
    val automationEngine = JarvisAutomationEngine(application, repository)
    val restClient = GeminiRestClient(preferencesManager, automationEngine)
    val liveWsClient = GeminiLiveWebSocketClient(preferencesManager, viewModelScope)
    val voiceEngine = JarvisVoiceEngine(application, preferencesManager, viewModelScope)

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isAccessibilityEnabled: StateFlow<Boolean> = JarvisAccessibilityService.isServiceActive

    private val _currentScreen = MutableStateFlow("MAIN")
    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    private val _latestResponse = MutableStateFlow("JARVIS online and fully operational, Sir.")
    private val _statusMessage = MutableStateFlow("ONLINE • STANDBY")

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    init {
        setupVoiceCallbacks()
        setupLiveWsCallbacks()
        observeStateUpdates()
    }

    private fun setupVoiceCallbacks() {
        voiceEngine.onUserCommandDetected = { command ->
            handleIncomingUserQuery(command, isVoice = true)
        }

        voiceEngine.onWakeWordActivated = {
            _statusMessage.value = "WAKE WORD DETECTED"
        }

        voiceEngine.onInterruptionTriggered = {
            liveWsClient.stopAudioPlayback()
            _statusMessage.value = "INTERRUPTED"
        }
    }

    private fun setupLiveWsCallbacks() {
        liveWsClient.onInterruptionDetected = {
            voiceEngine.interruptSpeaking()
        }

        liveWsClient.onTurnFinished = {
            if (voiceEngine.isCallingModeActive.value && preferencesManager.isCallingModeAutoListen()) {
                voiceEngine.startListeningContinuous()
            }
        }

        liveWsClient.onToolCallReceived = { name, args, callId ->
            viewModelScope.launch(Dispatchers.IO) {
                val result = executeToolFromLiveWs(name, args)
                liveWsClient.sendToolResponse(callId, name, result)
            }
        }
    }

    private fun observeStateUpdates() {
        viewModelScope.launch {
            combine(
                listOf(
                    _currentScreen,
                    voiceEngine.currentState,
                    voiceEngine.audioAmplitude,
                    voiceEngine.recognizedSnippet,
                    _latestResponse,
                    _pendingAction,
                    voiceEngine.isMicMuted,
                    voiceEngine.isSpeakerActive,
                    liveWsClient.connectionState,
                    isAccessibilityEnabled,
                    _statusMessage,
                    voiceEngine.isPttPressed,
                    voiceEngine.triggerMode
                )
            ) { values ->
                val screen = values[0] as String
                val vState = values[1] as JarvisState
                val amp = values[2] as Float
                val snippet = values[3] as String
                val resp = values[4] as String
                val pending = values[5] as PendingAction?
                val muted = values[6] as Boolean
                val speaker = values[7] as Boolean
                val liveState = values[8] as JarvisState
                val a11y = values[9] as Boolean
                val status = values[10] as String
                val ptt = values[11] as Boolean
                val trig = values[12] as String

                val effectiveState = if (liveState == JarvisState.CONNECTING) {
                    JarvisState.CONNECTING
                } else if (vState != JarvisState.STANDBY) {
                    vState
                } else if (liveState == JarvisState.CONNECTED) {
                    JarvisState.CONNECTED
                } else {
                    JarvisState.STANDBY
                }

                JarvisUiState(
                    currentScreen = screen,
                    jarvisState = effectiveState,
                    audioAmplitude = amp,
                    liveTranscript = snippet,
                    latestAiResponse = resp,
                    pendingAction = pending,
                    isMicMuted = muted,
                    isSpeakerActive = speaker,
                    isLiveWsConnected = liveState == JarvisState.CONNECTED,
                    isAccessibilityActive = a11y,
                    hasApiKey = preferencesManager.hasValidApiKey(),
                    statusMessage = status,
                    isPttPressed = ptt,
                    triggerMode = trig
                )
            }.collect { combinedState ->
                _uiState.value = combinedState
            }
        }
    }

    // --- Query and Command Execution ---

    fun handleIncomingUserQuery(userText: String, isVoice: Boolean = false) {
        if (userText.isBlank()) return

        viewModelScope.launch {
            // Check for Stop / Barge-in commands directly
            if (userText.equals("stop", ignoreCase = true) ||
                userText.equals("jarvis stop", ignoreCase = true) ||
                userText.equals("jarvis, stop", ignoreCase = true)
            ) {
                stopAssistant()
                return@launch
            }

            // Save user message to Room DB
            repository.insertMessage(
                ChatMessageEntity(role = "user", content = userText, isVoiceMessage = isVoice)
            )

            _statusMessage.value = "THINKING • PROCESSING DIRECTIVE"

            // Gather context & memory
            val memoryContext = if (preferencesManager.isMemoryEnabled()) {
                val memList = repository.getMemoriesList()
                if (memList.isNotEmpty()) {
                    "Long-term memory:\n" + memList.take(6).joinToString("\n") { "- [${it.category}] ${it.memoryKey}: ${it.memoryValue}" }
                } else ""
            } else ""

            // Recent history for chat context
            val recent = repository.getRecentMessages(6).reversed().map { it.role to it.content }

            when (val result = restClient.sendChat(userText, recent, memoryContext)) {
                is GeminiResult.Success -> {
                    _latestResponse.value = result.replyText
                    _statusMessage.value = "ONLINE • READY"

                    val toolSummary = if (result.executedTools.isNotEmpty()) {
                        result.executedTools.joinToString("\n")
                    } else null

                    // Save AI response to DB
                    repository.insertMessage(
                        ChatMessageEntity(
                            role = "assistant",
                            content = result.replyText,
                            toolCallInfo = toolSummary,
                            isVoiceMessage = isVoice
                        )
                    )

                    // Speak the response if in voice mode or calling mode
                    if (isVoice || voiceEngine.isCallingModeActive.value) {
                        voiceEngine.speak(result.replyText)
                    }
                }
                is GeminiResult.Error -> {
                    val errorReply = "I encountered an error: ${result.message}"
                    _latestResponse.value = errorReply
                    _statusMessage.value = "ERROR • ${result.message.take(24)}"
                    repository.insertMessage(
                        ChatMessageEntity(role = "assistant", content = errorReply, isVoiceMessage = isVoice)
                    )
                    if (isVoice || voiceEngine.isCallingModeActive.value) {
                        voiceEngine.speak(errorReply)
                    }
                }
            }
        }
    }

    private suspend fun executeToolFromLiveWs(name: String, args: org.json.JSONObject): String {
        return when (name) {
            "open_app" -> automationEngine.openApp(args.optString("app_name", ""))
            "search_web" -> automationEngine.searchWeb(args.optString("query", ""))
            "create_note" -> automationEngine.createNote(args.optString("title", "Voice Note"), args.optString("content", ""))
            "read_notes" -> automationEngine.readNotesSummary(args.optString("query", ""))
            "save_memory" -> automationEngine.saveMemoryFact(args.optString("category", "preference"), args.optString("key", ""), args.optString("value", ""))
            "adjust_volume" -> automationEngine.adjustVolume(args.optString("direction", "up"))
            "open_settings" -> automationEngine.openSettingsScreen(args.optString("setting_type", "main"))
            "read_screen" -> automationEngine.readScreen()?.readableText ?: "Screen reading not available."
            "tap_screen_element" -> if (automationEngine.tapScreenElement(args.optString("element_label", ""))) "Element tapped." else "Could not tap element."
            "scroll_screen" -> if (automationEngine.scrollScreen(args.optString("direction", "down") != "up")) "Scrolled." else "Could not scroll."
            "navigate_system" -> if (automationEngine.navigateSystem(args.optString("action", "back"))) "Navigated." else "Navigation failed."
            else -> "Unsupported tool $name."
        }
    }

    // --- Control Actions ---

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun startCallingMode() {
        _currentScreen.value = "CALLING"
        voiceEngine.enterCallingMode()
        // Try live WebSocket if user has API key
        if (preferencesManager.hasValidApiKey()) {
            liveWsClient.connect()
        }
    }

    fun exitCallingMode() {
        voiceEngine.exitCallingMode()
        liveWsClient.disconnect()
        _currentScreen.value = "MAIN"
    }

    fun stopAssistant() {
        voiceEngine.interruptSpeaking()
        liveWsClient.stopAudioPlayback()
        _statusMessage.value = "STOPPED • STANDING BY"
    }

    fun toggleMicMute() = voiceEngine.toggleMicMute()
    fun toggleSpeaker() = voiceEngine.toggleSpeaker()

    fun triggerOneShotVoiceListen() {
        voiceEngine.startListeningContinuous()
    }

    fun startPushToTalk() {
        voiceEngine.startPushToTalk()
    }

    fun stopPushToTalk() {
        voiceEngine.stopPushToTalk()
    }

    fun toggleTapToTalk() {
        voiceEngine.toggleTapToTalk()
    }

    fun setTriggerMode(mode: String) {
        voiceEngine.setTriggerMode(mode)
    }

    // --- Notes & Memory Methods ---

    fun createNote(title: String, content: String, tags: String = "general") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNote(NoteEntity(title = title, content = content, tags = tags))
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNoteById(id)
        }
    }

    fun clearNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearNotes()
        }
    }

    fun createMemory(category: String, key: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMemory(category, key, value)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMemoryById(id)
        }
    }

    fun clearMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearMemories()
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    // --- Settings & API Key ---

    fun saveApiKey(key: String) {
        preferencesManager.saveCustomApiKey(key)
    }

    fun clearApiKey() {
        preferencesManager.clearCustomApiKey()
    }

    fun testApiKey(testKey: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val keyToTest = testKey.trim().ifEmpty { preferencesManager.getEffectiveApiKey() }
            if (keyToTest.isBlank() || keyToTest == "MY_GEMINI_API_KEY") {
                onResult(false, "API Key is empty.")
                return@launch
            }
            try {
                val testUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$keyToTest"
                val body = org.json.JSONObject().apply {
                    put("contents", org.json.JSONArray().put(org.json.JSONObject().apply {
                        put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", "JARVIS connection ping")))
                    }))
                }
                val request = okhttp3.Request.Builder()
                    .url(testUrl)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                val response = okhttp3.OkHttpClient().newCall(request).execute()
                if (response.isSuccessful) {
                    onResult(true, "Authentication verified successfully! Gemini is ready.")
                } else {
                    onResult(false, "Authentication failed (HTTP ${response.code}). Check your key.")
                }
            } catch (e: Exception) {
                onResult(false, "Network test error: ${e.localizedMessage}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
        liveWsClient.release()
    }
}
