package com.example.network

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import com.example.data.local.PreferencesManager
import com.example.data.model.JarvisState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class GeminiLiveWebSocketClient(
    private val preferencesManager: PreferencesManager,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "GeminiLiveWS"
        private const val LIVE_ENDPOINT =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"

        private const val PLAYBACK_SAMPLE_RATE = 24000
    }

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    private val _connectionState = MutableStateFlow(JarvisState.STANDBY)
    val connectionState: StateFlow<JarvisState> = _connectionState.asStateFlow()

    private val _isLiveSpeaking = MutableStateFlow(false)
    val isLiveSpeaking: StateFlow<Boolean> = _isLiveSpeaking.asStateFlow()

    private val _transcriptFlow = MutableStateFlow("")
    val transcriptFlow: StateFlow<String> = _transcriptFlow.asStateFlow()

    // Audio Output Playback
    private var audioTrack: AudioTrack? = null
    private val audioQueue = ConcurrentLinkedQueue<ByteArray>()
    private var playbackJob: Job? = null
    private var isPlayingAudio = false

    var onInterruptionDetected: (() -> Unit)? = null
    var onTurnFinished: (() -> Unit)? = null
    var onToolCallReceived: ((name: String, args: JSONObject, callId: String) -> Unit)? = null

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                PLAYBACK_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = (minBufferSize * 4).coerceAtLeast(8192)

            audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(PLAYBACK_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed initializing AudioTrack", e)
        }
    }

    fun connect() {
        val apiKey = preferencesManager.getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            _connectionState.value = JarvisState.ERROR
            _transcriptFlow.value = "Gemini API key is required. Please set it in Settings."
            return
        }

        disconnect()
        _connectionState.value = JarvisState.CONNECTING
        Log.i(TAG, "Initiating Gemini Live WebSocket connection...")

        val request = Request.Builder()
            .url("$LIVE_ENDPOINT?key=$apiKey")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.i(TAG, "Gemini Live WebSocket opened successfully")
                _connectionState.value = JarvisState.CONNECTED
                sendSetupMessage(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "Gemini Live WebSocket closing: $code / $reason")
                _connectionState.value = JarvisState.STANDBY
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "Gemini Live WebSocket closed: $code / $reason")
                _connectionState.value = JarvisState.STANDBY
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Gemini Live WebSocket failure", t)
                _connectionState.value = JarvisState.ERROR
                _transcriptFlow.value = "Live connection error: ${t.localizedMessage ?: "Unknown error"}"
            }
        })
    }

    fun disconnect() {
        try {
            stopAudioPlayback()
            webSocket?.close(1000, "User disconnected")
            webSocket = null
            _connectionState.value = JarvisState.STANDBY
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting Live WebSocket", e)
        }
    }

    private fun sendSetupMessage(ws: WebSocket) {
        val rawModel = preferencesManager.getModelName()
        // Format model path
        val modelPath = if (rawModel.startsWith("models/")) rawModel else "models/$rawModel"
        val selectedVoice = preferencesManager.getLiveVoice()

        val personalityMode = preferencesManager.getPersonality()
        val personality = try {
            com.example.data.model.Personality.valueOf(personalityMode)
        } catch (e: Exception) {
            com.example.data.model.Personality.NORMAL
        }
        val systemInstruction = personality.buildSystemPrompt(userName = "Sir")

        val setupJson = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", modelPath)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", selectedVoice)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })
            })
        }

        Log.i(TAG, "Sending Live setup configuration: model=$modelPath, voice=$selectedVoice")
        ws.send(setupJson.toString())
    }

    // Stream user PCM 16kHz audio chunk to Gemini Live
    fun sendAudioPcmChunk(pcmData: ByteArray, length: Int) {
        val ws = webSocket ?: return
        if (_connectionState.value != JarvisState.CONNECTED) return

        try {
            val base64Data = Base64.encodeToString(pcmData, 0, length, Base64.NO_WRAP)
            val chunkMessage = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().put(JSONObject().apply {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64Data)
                    }))
                })
            }
            ws.send(chunkMessage.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending realtime audio chunk", e)
        }
    }

    fun sendTextMessage(text: String) {
        val ws = webSocket ?: return
        try {
            val message = JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turns", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("text", text)
                        }))
                    }))
                    put("turnComplete", true)
                })
            }
            ws.send(message.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text message over Live WS", e)
        }
    }

    fun sendToolResponse(callId: String, functionName: String, output: String) {
        val ws = webSocket ?: return
        try {
            val message = JSONObject().apply {
                put("toolResponse", JSONObject().apply {
                    put("functionResponses", JSONArray().put(JSONObject().apply {
                        put("id", callId)
                        put("response", JSONObject().apply {
                            put("output", output)
                        })
                    }))
                })
            }
            ws.send(message.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending tool response over Live WS", e)
        }
    }

    private fun handleIncomingMessage(rawJson: String) {
        try {
            val root = JSONObject(rawJson)

            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                // Interruption notification from server
                if (serverContent.optBoolean("interrupted", false)) {
                    Log.i(TAG, "Server reported interruption!")
                    stopAudioPlayback()
                    onInterruptionDetected?.invoke()
                    return
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts") ?: JSONArray()

                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)

                        // Text part
                        if (part.has("text")) {
                            val txt = part.getString("text")
                            _transcriptFlow.value = txt
                        }

                        // Audio part
                        if (part.has("inlineData")) {
                            val inlineData = part.getJSONObject("inlineData")
                            val base64 = inlineData.getString("data")
                            val pcmBytes = Base64.decode(base64, Base64.DEFAULT)
                            queueAudioForPlayback(pcmBytes)
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    Log.i(TAG, "Gemini Live turn complete")
                    onTurnFinished?.invoke()
                }
            }

            // Tool Call from Server
            if (root.has("toolCall")) {
                val toolCall = root.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls") ?: JSONArray()
                for (i in 0 until functionCalls.length()) {
                    val fc = functionCalls.getJSONObject(i)
                    val name = fc.getString("name")
                    val args = fc.optJSONObject("args") ?: JSONObject()
                    val id = fc.optString("id", "")
                    onToolCallReceived?.invoke(name, args, id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini Live message: $rawJson", e)
        }
    }

    private fun queueAudioForPlayback(pcm: ByteArray) {
        audioQueue.add(pcm)
        if (!isPlayingAudio) {
            startAudioPlaybackLoop()
        }
    }

    private fun startAudioPlaybackLoop() {
        isPlayingAudio = true
        _isLiveSpeaking.value = true
        playbackJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack?.play()
                }
                while (isPlayingAudio) {
                    val chunk = audioQueue.poll()
                    if (chunk != null) {
                        audioTrack?.write(chunk, 0, chunk.size)
                    } else {
                        // Empty queue check
                        kotlinx.coroutines.delay(20)
                        if (audioQueue.isEmpty()) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio playback loop", e)
            } finally {
                isPlayingAudio = false
                _isLiveSpeaking.value = false
            }
        }
    }

    // Natural interruption / barge-in
    fun stopAudioPlayback() {
        try {
            audioQueue.clear()
            isPlayingAudio = false
            _isLiveSpeaking.value = false
            playbackJob?.cancel()
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
            Log.i(TAG, "Audio playback immediately interrupted and flushed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio playback", e)
        }
    }

    fun release() {
        disconnect()
        try {
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack", e)
        }
    }
}
