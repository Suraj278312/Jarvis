package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_MODEL_NAME = "gemini_model_name"
        private const val KEY_LIVE_VOICE = "gemini_live_voice"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_WAKE_PHRASE = "wake_phrase"
        private const val KEY_WAKE_SENSITIVITY = "wake_sensitivity"
        private const val KEY_BARGE_IN_ENABLED = "barge_in_enabled"
        private const val KEY_CALLING_MODE_AUTO_LISTEN = "calling_mode_auto_listen"
        private const val KEY_PERSONALITY = "personality_mode"
        private const val KEY_MEMORY_ENABLED = "memory_enabled"
        private const val KEY_CONFIRM_SENSITIVE = "confirm_sensitive_actions"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_TRIGGER_MODE = "mic_trigger_mode" // "BOTH", "PTT", "WAKE_WORD"

        const val DEFAULT_MODEL = "gemini-3.8-live"
        const val DEFAULT_VOICE = "Puck"
        const val DEFAULT_PERSONALITY = "NORMAL"
        const val TRIGGER_MODE_BOTH = "BOTH"
        const val TRIGGER_MODE_PTT = "PTT"
        const val TRIGGER_MODE_WAKE_WORD = "WAKE_WORD"
    }

    private val _apiKeyFlow = MutableStateFlow(getEffectiveApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    fun getCustomApiKey(): String {
        return prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
    }

    fun getEffectiveApiKey(): String {
        val custom = getCustomApiKey()
        if (custom.isNotBlank() && custom != "MY_GEMINI_API_KEY") {
            return custom
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return custom
    }

    fun hasValidApiKey(): Boolean {
        val key = getEffectiveApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
        _apiKeyFlow.value = getEffectiveApiKey()
    }

    fun clearCustomApiKey() {
        prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
        _apiKeyFlow.value = getEffectiveApiKey()
    }

    fun getModelName(): String {
        return prefs.getString(KEY_MODEL_NAME, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun setModelName(model: String) {
        prefs.edit().putString(KEY_MODEL_NAME, model.trim()).apply()
    }

    fun getLiveVoice(): String {
        return prefs.getString(KEY_LIVE_VOICE, DEFAULT_VOICE) ?: DEFAULT_VOICE
    }

    fun setLiveVoice(voice: String) {
        prefs.edit().putString(KEY_LIVE_VOICE, voice).apply()
    }

    fun isWakeWordEnabled(): Boolean {
        return prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
    }

    fun getWakePhrase(): String {
        return prefs.getString(KEY_WAKE_PHRASE, "JARVIS") ?: "JARVIS"
    }

    fun setWakePhrase(phrase: String) {
        prefs.edit().putString(KEY_WAKE_PHRASE, phrase).apply()
    }

    fun getWakeSensitivity(): String {
        return prefs.getString(KEY_WAKE_SENSITIVITY, "Normal") ?: "Normal"
    }

    fun setWakeSensitivity(sensitivity: String) {
        prefs.edit().putString(KEY_WAKE_SENSITIVITY, sensitivity).apply()
    }

    fun isBargeInEnabled(): Boolean {
        return prefs.getBoolean(KEY_BARGE_IN_ENABLED, true)
    }

    fun setBargeInEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BARGE_IN_ENABLED, enabled).apply()
    }

    fun isCallingModeAutoListen(): Boolean {
        return prefs.getBoolean(KEY_CALLING_MODE_AUTO_LISTEN, true)
    }

    fun setCallingModeAutoListen(autoListen: Boolean) {
        prefs.edit().putBoolean(KEY_CALLING_MODE_AUTO_LISTEN, autoListen).apply()
    }

    fun getPersonality(): String {
        return prefs.getString(KEY_PERSONALITY, DEFAULT_PERSONALITY) ?: DEFAULT_PERSONALITY
    }

    fun setPersonality(personality: String) {
        prefs.edit().putString(KEY_PERSONALITY, personality).apply()
    }

    fun isMemoryEnabled(): Boolean {
        return prefs.getBoolean(KEY_MEMORY_ENABLED, true)
    }

    fun setMemoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MEMORY_ENABLED, enabled).apply()
    }

    fun isConfirmSensitiveActions(): Boolean {
        return prefs.getBoolean(KEY_CONFIRM_SENSITIVE, true)
    }

    fun setConfirmSensitiveActions(confirm: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_SENSITIVE, confirm).apply()
    }

    fun getSpeechRate(): Float {
        return prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
    }

    fun setSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
    }

    fun getTriggerMode(): String {
        return prefs.getString(KEY_TRIGGER_MODE, TRIGGER_MODE_BOTH) ?: TRIGGER_MODE_BOTH
    }

    fun setTriggerMode(mode: String) {
        prefs.edit().putString(KEY_TRIGGER_MODE, mode).apply()
    }
}
