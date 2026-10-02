package com.example.network

import android.util.Log
import com.example.data.local.PreferencesManager
import com.example.service.JarvisAutomationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(
        val replyText: String,
        val executedTools: List<String> = emptyList(),
        val pendingConfirmation: JSONObject? = null
    ) : GeminiResult()

    data class Error(val message: String, val isNetworkError: Boolean = false) : GeminiResult()
}

class GeminiRestClient(
    private val preferencesManager: PreferencesManager,
    private val automationEngine: JarvisAutomationEngine
) {
    companion object {
        private const val TAG = "GeminiRestClient"
        private const val BASE_REST_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun sendChat(
        userMessage: String,
        history: List<Pair<String, String>>, // role to text
        rememberedContext: String = ""
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = preferencesManager.getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult.Error(
                "Gemini API Key is missing. Please configure your API key in Settings."
            )
        }

        val modelName = preferencesManager.getModelName().let {
            if (it.contains("live", ignoreCase = true)) {
                // If a live-only model was selected for REST, use gemini-3.5-flash or flash-latest
                "gemini-3.5-flash"
            } else {
                it
            }
        }

        val url = "$BASE_REST_URL$modelName:generateContent?key=$apiKey"

        val personalityMode = preferencesManager.getPersonality()
        val personality = try {
            com.example.data.model.Personality.valueOf(personalityMode)
        } catch (e: Exception) {
            com.example.data.model.Personality.NORMAL
        }
        val systemPrompt = personality.buildSystemPrompt(
            userName = "Sir",
            rememberedContext = if (preferencesManager.isMemoryEnabled()) rememberedContext else ""
        )

        try {
            val requestJson = buildFullRequest(userMessage, history, systemPrompt)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                return@withContext GeminiResult.Error(errorMsg, response.code >= 500)
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResult.Error("No candidates returned from Gemini.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts") ?: JSONArray()

            val executedTools = mutableListOf<String>()
            val textParts = mutableListOf<String>()
            var functionCallJson: JSONObject? = null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    textParts.add(part.getString("text"))
                }
                if (part.has("functionCall")) {
                    functionCallJson = part.getJSONObject("functionCall")
                }
            }

            // If a tool function call was requested, execute it!
            if (functionCallJson != null) {
                val funcName = functionCallJson.getString("name")
                val args = functionCallJson.optJSONObject("args") ?: JSONObject()
                Log.i(TAG, "Gemini requested tool: $funcName with args: $args")

                val toolExecutionResult = executeToolLocally(funcName, args)
                executedTools.add("$funcName -> $toolExecutionResult")

                // Send tool execution result back to Gemini to get the conversational response
                val followupResult = sendToolResponseFollowup(
                    url = url,
                    originalRequest = requestJson,
                    modelCandidateContent = content ?: JSONObject(),
                    functionName = funcName,
                    toolResult = toolExecutionResult
                )
                return@withContext when (followupResult) {
                    is GeminiResult.Success -> followupResult.copy(
                        executedTools = executedTools + followupResult.executedTools
                    )
                    is GeminiResult.Error -> GeminiResult.Success(
                        replyText = "Action completed: $toolExecutionResult",
                        executedTools = executedTools
                    )
                }
            }

            val reply = textParts.joinToString("\n").trim()
            GeminiResult.Success(
                replyText = reply.ifEmpty { "Understood, Sir." },
                executedTools = executedTools
            )
        } catch (e: Exception) {
            Log.e(TAG, "Network or parsing error calling Gemini", e)
            GeminiResult.Error(
                message = e.localizedMessage ?: "Network connection error.",
                isNetworkError = true
            )
        }
    }

    private suspend fun executeToolLocally(name: String, args: JSONObject): String {
        return try {
            when (name) {
                "open_app" -> {
                    val appName = args.optString("app_name", "")
                    automationEngine.openApp(appName)
                }
                "open_settings" -> {
                    val setting = args.optString("setting_type", "main")
                    automationEngine.openSettingsScreen(setting)
                }
                "adjust_volume" -> {
                    val direction = args.optString("direction", "up")
                    automationEngine.adjustVolume(direction)
                }
                "search_web" -> {
                    val query = args.optString("query", "")
                    automationEngine.searchWeb(query)
                }
                "read_screen" -> {
                    val info = automationEngine.readScreen()
                    if (info != null) {
                        "Visible screen content:\n${info.readableText}\nClickable elements: ${info.clickableElements.take(8).joinToString()}"
                    } else {
                        "Screen reading is not available. Please ensure the JARVIS Accessibility Service is turned on in Android Settings."
                    }
                }
                "tap_screen_element" -> {
                    val target = args.optString("element_label", "")
                    val tapped = automationEngine.tapScreenElement(target)
                    if (tapped) "Tapped element '$target' on screen." else "Could not locate or tap element '$target'."
                }
                "type_text" -> {
                    val text = args.optString("text", "")
                    val hint = args.optString("field_hint", "")
                    val typed = automationEngine.typeIntoField(text, if (hint.isBlank()) null else hint)
                    if (typed) "Typed '$text' into active input field." else "Could not focus or type into input field."
                }
                "scroll_screen" -> {
                    val direction = args.optString("direction", "down")
                    val scrolled = automationEngine.scrollScreen(direction.lowercase() != "up")
                    if (scrolled) "Scrolled screen $direction." else "Unable to scroll screen."
                }
                "navigate_system" -> {
                    val action = args.optString("action", "back")
                    val nav = automationEngine.navigateSystem(action)
                    if (nav) "Navigated $action." else "Navigation action failed. Ensure Accessibility Service is enabled."
                }
                "create_note" -> {
                    val title = args.optString("title", "Voice Note")
                    val content = args.optString("content", "")
                    automationEngine.createNote(title, content)
                }
                "read_notes" -> {
                    val query = args.optString("query", "")
                    automationEngine.readNotesSummary(if (query.isBlank()) null else query)
                }
                "save_memory" -> {
                    val category = args.optString("category", "preference")
                    val key = args.optString("key", "")
                    val value = args.optString("value", "")
                    automationEngine.saveMemoryFact(category, key, value)
                }
                "make_phone_call" -> {
                    val contact = args.optString("contact_or_number", "")
                    automationEngine.makePhoneCall(contact)
                }
                "send_sms" -> {
                    val contact = args.optString("contact_or_number", "")
                    val message = args.optString("message", "")
                    automationEngine.sendSms(contact, message)
                }
                else -> "Unknown tool '$name'."
            }
        } catch (e: Exception) {
            "Tool execution error: ${e.message}"
        }
    }

    private suspend fun sendToolResponseFollowup(
        url: String,
        originalRequest: JSONObject,
        modelCandidateContent: JSONObject,
        functionName: String,
        toolResult: String
    ): GeminiResult = withContext(Dispatchers.IO) {
        try {
            val contents = originalRequest.getJSONArray("contents")
            // Append model candidate with function call
            contents.put(modelCandidateContent)

            // Append user function response
            val toolResponsePart = JSONObject().apply {
                put("functionResponse", JSONObject().apply {
                    put("name", functionName)
                    put("response", JSONObject().apply {
                        put("output", toolResult)
                    })
                })
            }
            val toolResponseContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(toolResponsePart))
            }
            contents.put(toolResponseContent)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = originalRequest.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GeminiResult.Error("Tool followup error: ${response.code}")
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
            val textBuilder = StringBuilder()
            for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                if (p.has("text")) {
                    textBuilder.append(p.getString("text")).append(" ")
                }
            }
            GeminiResult.Success(textBuilder.toString().trim().ifEmpty { toolResult })
        } catch (e: Exception) {
            GeminiResult.Error("Followup failed: ${e.message}")
        }
    }

    private fun buildFullRequest(
        userMessage: String,
        history: List<Pair<String, String>>,
        systemPrompt: String
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        val sysInstruction = JSONObject().apply {
            put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
        }
        root.put("systemInstruction", sysInstruction)

        // Contents
        val contentsArray = JSONArray()
        for ((role, text) in history) {
            val contentObj = JSONObject().apply {
                put("role", if (role.lowercase() == "user") "user" else "model")
                put("parts", JSONArray().put(JSONObject().put("text", text)))
            }
            contentsArray.put(contentObj)
        }
        // Current user message
        val currentObj = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
        }
        contentsArray.put(currentObj)
        root.put("contents", contentsArray)

        // Tools
        root.put("tools", buildToolsDeclarations())

        return root
    }

    private fun buildToolsDeclarations(): JSONArray {
        val functionDeclarations = JSONArray()

        functionDeclarations.put(JSONObject().apply {
            put("name", "open_app")
            put("description", "Launch any installed application on the user's phone, such as YouTube, Camera, Chrome, Maps, Settings.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("app_name", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The name or package of the app to launch (e.g. 'YouTube', 'Camera', 'com.google.android.youtube').")
                    })
                })
                put("required", JSONArray().put("app_name"))
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "open_settings")
            put("description", "Open system settings screens (wifi, bluetooth, accessibility, sound, display).")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("setting_type", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The setting screen to open ('wifi', 'bluetooth', 'accessibility', 'sound', 'display').")
                    })
                })
                put("required", JSONArray().put("setting_type"))
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "adjust_volume")
            put("description", "Adjust device media volume up, down, or mute.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("direction", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'up', 'down', or 'mute'.")
                    })
                })
                put("required", JSONArray().put("direction"))
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "search_web")
            put("description", "Perform a Google search on the web or device.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The search query.")
                    })
                })
                put("required", JSONArray().put("query"))
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "read_screen")
            put("description", "Inspect visible text and interactive UI elements on the current Android screen using Accessibility Service.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "tap_screen_element")
            put("description", "Tap or click a UI element on the screen matching the given label, text, or button description.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("element_label", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The text or description of the button/element to tap.")
                    })
                })
                put("required", JSONArray().put("element_label"))
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "type_text")
            put("description", "Type text into the currently focused or specified input field.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("text", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The text to type into the field.")
                    })
                    put("field_hint", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional hint or name of the field.")
                    })
                })
                put("required", JSONArray().put("text"))
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "scroll_screen")
            put("description", "Scroll the current screen content up or down.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("direction", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'up' or 'down'.")
                    })
                })
                put("required", JSONArray().put("direction"))
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "navigate_system")
            put("description", "Perform Android global system navigation: back, home, recents, or notifications.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("action", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'back', 'home', 'recents', or 'notifications'.")
                    })
                })
                put("required", JSONArray().put("action"))
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "create_note")
            put("description", "Create and save a new note in JARVIS memory.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("title", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Title of the note.")
                    })
                    put("content", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Content of the note.")
                    })
                })
                put("required", JSONArray().put("title").put("content"))
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "read_notes")
            put("description", "Search or list notes from JARVIS memory.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional search keyword to filter notes.")
                    })
                })
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "save_memory")
            put("description", "Save long-term user preference, fact, or important information.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("category", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'preference', 'fact', or 'summary'.")
                    })
                    put("key", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Key or subject of the memory.")
                    })
                    put("value", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The information to remember.")
                    })
                })
                put("required", JSONArray().put("key").put("value"))
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "make_phone_call")
            put("description", "Place or prepare a phone call to a contact or number.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("contact_or_number", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Contact name or phone number.")
                    })
                })
                put("required", JSONArray().put("contact_or_number"))
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "send_sms")
            put("description", "Prepare or send an SMS text message to a contact.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("contact_or_number", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Contact name or phone number.")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Message body to send.")
                    })
                })
                put("required", JSONArray().put("contact_or_number").put("message"))
            })
        })

        val toolContainer = JSONObject().apply {
            put("functionDeclarations", functionDeclarations)
        }
        return JSONArray().put(toolContainer)
    }

    private fun parseErrorMessage(responseBody: String?, statusCode: Int): String {
        if (responseBody.isNullOrBlank()) {
            return "Server returned HTTP status $statusCode."
        }
        return try {
            val json = JSONObject(responseBody)
            val error = json.optJSONObject("error")
            val message = error?.optString("message")
            message ?: "Error code: $statusCode"
        } catch (e: Exception) {
            "HTTP $statusCode: $responseBody"
        }
    }
}
