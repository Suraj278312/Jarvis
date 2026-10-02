package com.example.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.local.PreferencesManager
import com.example.data.model.JarvisState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class JarvisVoiceEngine(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val coroutineScope: CoroutineScope
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "JarvisVoiceEngine"
        private const val UTTERANCE_ID = "jarvis_speech_out"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _currentState = MutableStateFlow(JarvisState.STANDBY)
    val currentState: StateFlow<JarvisState> = _currentState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0.1f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _recognizedSnippet = MutableStateFlow("")
    val recognizedSnippet: StateFlow<String> = _recognizedSnippet.asStateFlow()

    private val _isCallingModeActive = MutableStateFlow(false)
    val isCallingModeActive: StateFlow<Boolean> = _isCallingModeActive.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isSpeakerActive = MutableStateFlow(true)
    val isSpeakerActive: StateFlow<Boolean> = _isSpeakerActive.asStateFlow()

    private val _isPttPressed = MutableStateFlow(false)
    val isPttPressed: StateFlow<Boolean> = _isPttPressed.asStateFlow()

    private val _triggerMode = MutableStateFlow(preferencesManager.getTriggerMode())
    val triggerMode: StateFlow<String> = _triggerMode.asStateFlow()

    var onUserCommandDetected: ((command: String) -> Unit)? = null
    var onWakeWordActivated: (() -> Unit)? = null
    var onInterruptionTriggered: (() -> Unit)? = null

    init {
        initSpeechRecognizer()
        textToSpeech = TextToSpeech(context.applicationContext, this)
        // If wake word is active on launch, arm standby listening
        checkAndArmWakeWord()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "TTS Language not supported")
            } else {
                isTtsReady = true
                configureTts()
            }
        } else {
            Log.e(TAG, "TTS Initialization failed")
        }
    }

    private fun configureTts() {
        val tts = textToSpeech ?: return
        tts.setPitch(0.95f) // Slightly deeper, sophisticated JARVIS tone
        tts.setSpeechRate(preferencesManager.getSpeechRate())

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _currentState.value = JarvisState.SPEAKING
            }

            override fun onDone(utteranceId: String?) {
                Log.i(TAG, "JARVIS finished speaking utterance.")
                handleSpeakingFinished()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS Utterance error")
                handleSpeakingFinished()
            }
        })
    }

    private fun handleSpeakingFinished() {
        // Continuous Conversation Loop: Automatically resume listening if in Calling Mode!
        if (_isCallingModeActive.value && preferencesManager.isCallingModeAutoListen()) {
            coroutineScope.launch(Dispatchers.Main) {
                startListeningContinuous()
            }
        } else {
            _currentState.value = JarvisState.STANDBY
            checkAndArmWakeWord()
        }
    }

    private fun initSpeechRecognizer() {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.destroy()

                // Try to explicitly bind to Google's Speech Recognition Service if installed
                val googleSearchComponent = ComponentName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
                )

                speechRecognizer = try {
                    if (SpeechRecognizer.isRecognitionAvailable(context)) {
                        SpeechRecognizer.createSpeechRecognizer(context, googleSearchComponent)
                    } else {
                        SpeechRecognizer.createSpeechRecognizer(context)
                    }
                } catch (e: Exception) {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }

                speechRecognizer?.setRecognitionListener(createRecognitionListener())
                Log.i(TAG, "Google Speech Recognizer successfully initialized.")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Google Speech Recognizer", e)
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (_currentState.value != JarvisState.SPEAKING) {
                    _currentState.value = JarvisState.LISTENING
                }
            }

            override fun onBeginningOfSpeech() {
                // If user starts talking while JARVIS is speaking -> BARGE-IN INTERRUPTION!
                if (_currentState.value == JarvisState.SPEAKING) {
                    if (preferencesManager.isBargeInEnabled()) {
                        Log.i(TAG, "Barge-in speech detected while speaking! Interrupting.")
                        interruptSpeaking()
                    }
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                _audioAmplitude.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (_currentState.value == JarvisState.LISTENING && !_isPttPressed.value) {
                    _currentState.value = JarvisState.THINKING
                }
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    else -> "Recognizer code $error"
                }
                Log.d(TAG, "SpeechRecognizer status: $errorMsg ($error)")

                // In Calling Mode: keep listening continuously
                if (_isCallingModeActive.value && !_isMicMuted.value) {
                    coroutineScope.launch(Dispatchers.Main) {
                        kotlinx.coroutines.delay(300)
                        startListeningContinuous()
                    }
                } else if (_currentState.value == JarvisState.LISTENING && !_isPttPressed.value) {
                    _currentState.value = JarvisState.STANDBY
                    checkAndArmWakeWord()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim() ?: ""
                Log.i(TAG, "Google Speech result: '$recognizedText'")
                handleRecognizedSpeech(recognizedText)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                _recognizedSnippet.value = partial

                // Immediate check for "stop" commands during barge-in
                if (_currentState.value == JarvisState.SPEAKING) {
                    if (isStopCommand(partial)) {
                        Log.i(TAG, "Stop command in partial results: '$partial'")
                        interruptSpeaking()
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun handleRecognizedSpeech(text: String) {
        if (text.isBlank()) {
            if (_isCallingModeActive.value) {
                startListeningContinuous()
            } else {
                checkAndArmWakeWord()
            }
            return
        }

        _recognizedSnippet.value = text

        // 1. Interruption / Stop Command
        if (isStopCommand(text)) {
            interruptSpeaking()
            speak("Understood, Sir. Standing by.")
            return
        }

        // 2. Wake-word Handling in STANDBY mode
        val currentMode = preferencesManager.getTriggerMode()
        val wakeWordAllowed = currentMode == PreferencesManager.TRIGGER_MODE_BOTH ||
                currentMode == PreferencesManager.TRIGGER_MODE_WAKE_WORD

        if (!_isCallingModeActive.value && !_isPttPressed.value && wakeWordAllowed) {
            val wakePhrase = preferencesManager.getWakePhrase().lowercase()
            val lowerText = text.lowercase()

            val isWakeWordDetected = lowerText.startsWith(wakePhrase) ||
                    lowerText.contains("jarvis") ||
                    lowerText.contains("hey jarvis") ||
                    lowerText.contains("ok jarvis")

            if (isWakeWordDetected) {
                vibrateBrief(60)
                onWakeWordActivated?.invoke()

                val command = lowerText
                    .replace("hey jarvis", "")
                    .replace("ok jarvis", "")
                    .replace("jarvis", "")
                    .trim()

                if (command.isNotBlank()) {
                    _currentState.value = JarvisState.THINKING
                    onUserCommandDetected?.invoke(command)
                } else {
                    speak("Yes Sir, I am listening.")
                }
                return
            }
        }

        // 3. User Directive from Push-to-Talk or Continuous loop
        _currentState.value = JarvisState.THINKING
        onUserCommandDetected?.invoke(text)
    }

    private fun isStopCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower == "stop" ||
                lower == "jarvis stop" ||
                lower == "jarvis, stop" ||
                lower == "cancel" ||
                lower == "shut up" ||
                lower == "quiet" ||
                lower == "pause"
    }

    // --- Push-to-Talk (PTT) Controls ---

    fun startPushToTalk() {
        if (_isMicMuted.value) return
        _isPttPressed.value = true
        vibrateBrief(40)
        Log.i(TAG, "Push-to-Talk TRIGGERED")

        coroutineScope.launch(Dispatchers.Main) {
            try {
                val intent = buildGoogleRecognizerIntent(isPtt = true)
                speechRecognizer?.cancel()
                speechRecognizer?.startListening(intent)
                _currentState.value = JarvisState.LISTENING
            } catch (e: Exception) {
                Log.e(TAG, "Error starting Push-to-Talk", e)
            }
        }
    }

    fun stopPushToTalk() {
        if (!_isPttPressed.value) return
        _isPttPressed.value = false
        vibrateBrief(30)
        Log.i(TAG, "Push-to-Talk RELEASED - Submitting to Gemini")

        coroutineScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping Push-to-Talk", e)
            }
        }
    }

    fun toggleTapToTalk() {
        if (_currentState.value == JarvisState.LISTENING) {
            stopListening()
        } else {
            startPushToTalk()
        }
    }

    // --- Continuous / Wake Word Standby Listening ---

    fun startListeningContinuous() {
        if (_isMicMuted.value) return

        coroutineScope.launch(Dispatchers.Main) {
            try {
                val intent = buildGoogleRecognizerIntent(isPtt = false)
                speechRecognizer?.cancel()
                speechRecognizer?.startListening(intent)
                _currentState.value = JarvisState.LISTENING
                Log.d(TAG, "Google Speech Recognizer continuous listening started.")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting continuous speech recognition", e)
            }
        }
    }

    fun stopListening() {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                _isPttPressed.value = false
                if (_currentState.value == JarvisState.LISTENING) {
                    _currentState.value = JarvisState.STANDBY
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognition", e)
            }
        }
    }

    private fun checkAndArmWakeWord() {
        val currentMode = preferencesManager.getTriggerMode()
        val wakeWordAllowed = currentMode == PreferencesManager.TRIGGER_MODE_BOTH ||
                currentMode == PreferencesManager.TRIGGER_MODE_WAKE_WORD

        if (preferencesManager.isWakeWordEnabled() && wakeWordAllowed && !_isCallingModeActive.value && !_isMicMuted.value) {
            coroutineScope.launch(Dispatchers.Main) {
                kotlinx.coroutines.delay(400)
                try {
                    val intent = buildGoogleRecognizerIntent(isPtt = false)
                    speechRecognizer?.cancel()
                    speechRecognizer?.startListening(intent)
                    Log.d(TAG, "Wake word background detection armed for 'JARVIS'")
                } catch (e: Exception) {
                    Log.d(TAG, "Unable to arm wake word: ${e.message}")
                }
            }
        }
    }

    private fun buildGoogleRecognizerIntent(isPtt: Boolean): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (isPtt) {
                // PTT: keep listening until user releases button or pauses
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            } else {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            }
        }
    }

    fun setTriggerMode(mode: String) {
        preferencesManager.setTriggerMode(mode)
        _triggerMode.value = mode
        if (mode == PreferencesManager.TRIGGER_MODE_PTT) {
            stopListening()
        } else {
            checkAndArmWakeWord()
        }
    }

    // --- Audio Feedback & Playback ---

    fun speak(text: String) {
        if (!_isSpeakerActive.value) {
            handleSpeakingFinished()
            return
        }

        coroutineScope.launch(Dispatchers.Main) {
            if (!isTtsReady) {
                Log.w(TAG, "TTS not ready yet")
                handleSpeakingFinished()
                return@launch
            }

            _currentState.value = JarvisState.SPEAKING
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
    }

    fun interruptSpeaking() {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                textToSpeech?.stop()
                _currentState.value = JarvisState.INTERRUPTED
                onInterruptionTriggered?.invoke()
                vibrateBrief(30)
                Log.i(TAG, "JARVIS speech output interrupted.")

                if (_isCallingModeActive.value) {
                    kotlinx.coroutines.delay(300)
                    startListeningContinuous()
                } else {
                    _currentState.value = JarvisState.STANDBY
                    checkAndArmWakeWord()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error interrupting speech", e)
            }
        }
    }

    private fun vibrateBrief(millis: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(millis)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Vibrate unavailable")
        }
    }

    // --- Calling Mode Controls ---

    fun enterCallingMode() {
        _isCallingModeActive.value = true
        _isMicMuted.value = false
        startListeningContinuous()
        speak("Calling mode engaged. Continuous audio channel open, Sir.")
    }

    fun exitCallingMode() {
        _isCallingModeActive.value = false
        stopListening()
        interruptSpeaking()
        _currentState.value = JarvisState.STANDBY
        checkAndArmWakeWord()
    }

    fun toggleMicMute() {
        val next = !_isMicMuted.value
        _isMicMuted.value = next
        if (next) {
            stopListening()
        } else {
            startListeningContinuous()
        }
    }

    fun toggleSpeaker() {
        val next = !_isSpeakerActive.value
        _isSpeakerActive.value = next
        if (!next) {
            textToSpeech?.stop()
        }
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing voice engine", e)
        }
    }
}
