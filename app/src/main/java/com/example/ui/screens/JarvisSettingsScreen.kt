package com.example.ui.screens

import android.widget.Toast
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Personality
import com.example.data.model.VoiceOptions
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceBorder
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.viewmodel.JarvisViewModel

@Composable
fun JarvisSettingsScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo("MAIN")
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val prefs = viewModel.preferencesManager

    var customApiKey by remember { mutableStateOf(prefs.getCustomApiKey()) }
    var isApiKeyObscured by remember { mutableStateOf(true) }
    var testResultFeedback by remember { mutableStateOf<String?>(null) }
    var isTestingKey by remember { mutableStateOf(false) }

    var modelName by remember { mutableStateOf(prefs.getModelName()) }
    var selectedVoice by remember { mutableStateOf(prefs.getLiveVoice()) }
    var wakeWordEnabled by remember { mutableStateOf(prefs.isWakeWordEnabled()) }
    var wakePhrase by remember { mutableStateOf(prefs.getWakePhrase()) }
    var bargeInEnabled by remember { mutableStateOf(prefs.isBargeInEnabled()) }
    var callingModeAutoListen by remember { mutableStateOf(prefs.isCallingModeAutoListen()) }
    var personalityMode by remember { mutableStateOf(prefs.getPersonality()) }
    var confirmSensitiveActions by remember { mutableStateOf(prefs.isConfirmSensitiveActions()) }
    var speechRate by remember { mutableFloatStateOf(prefs.getSpeechRate()) }
    var triggerMode by remember { mutableStateOf(prefs.getTriggerMode()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("MAIN") },
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = JarvisCyan
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "SYSTEM CONFIGURATION",
                    color = JarvisCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "AI Brain • Voice • Wake Word • Security",
                    color = JarvisTextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 1. AI BRAIN & API KEY ---
        SettingsSectionHeader(title = "CORE AI BRAIN (GEMINI)", icon = Icons.Default.Key)

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gemini API Key",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Stored securely on device. Used for Live WebSocket and REST intelligence.",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = { customApiKey = it },
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                    placeholder = { Text("AIzaSy...", color = JarvisTextMuted, fontSize = 12.sp) },
                    visualTransformation = if (isApiKeyObscured) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyObscured = !isApiKeyObscured }) {
                            Icon(
                                imageVector = if (isApiKeyObscured) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle visibility",
                                tint = JarvisCyan
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisSurfaceBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save Key
                    Button(
                        onClick = {
                            viewModel.saveApiKey(customApiKey)
                            Toast.makeText(context, "API Key saved successfully.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisBackground),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Test Key
                    Button(
                        onClick = {
                            isTestingKey = true
                            testResultFeedback = "Testing connection to Gemini..."
                            viewModel.testApiKey(customApiKey) { success, msg ->
                                isTestingKey = false
                                testResultFeedback = msg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant, contentColor = JarvisCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isTestingKey) "Testing..." else "Test Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Delete Key
                    Button(
                        onClick = {
                            customApiKey = ""
                            viewModel.clearApiKey()
                            testResultFeedback = "API Key removed."
                            Toast.makeText(context, "Key removed.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1518), contentColor = JarvisRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Key", modifier = Modifier.size(16.dp))
                    }
                }

                if (testResultFeedback != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testResultFeedback!!,
                        color = if (testResultFeedback!!.contains("verified", ignoreCase = true)) JarvisGreen else JarvisAmber,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Configurable Model Name
                Text(
                    text = "Configurable Gemini Model",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Selected model for Live voice streaming & task reasoning.",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = modelName,
                    onValueChange = {
                        modelName = it
                        prefs.setModelName(it)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("model_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisSurfaceBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Model quick selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf("gemini-3.8-live", "gemini-2.5-flash-native-audio-preview-12-2025", "gemini-3.5-flash")
                    for (preset in presets) {
                        val isSelected = modelName.equals(preset, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) JarvisCyan.copy(alpha = 0.2f) else JarvisSurfaceVariant)
                                .border(1.dp, if (isSelected) JarvisCyan else JarvisSurfaceBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    modelName = preset
                                    prefs.setModelName(preset)
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = preset.split("-").take(2).joinToString("-"),
                                color = if (isSelected) JarvisCyan else JarvisTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 2. VOICE & CALLING SETTINGS ---
        SettingsSectionHeader(title = "GEMINI LIVE VOICE & AUDIO", icon = Icons.Default.RecordVoiceOver)

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Live Synthesized Voice",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Native Gemini Live voice persona output.",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                for (voice in VoiceOptions.AVAILABLE) {
                    val isSelected = selectedVoice.equals(voice.id, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) JarvisCyan.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (isSelected) JarvisCyan else JarvisSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedVoice = voice.id
                                prefs.setLiveVoice(voice.id)
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${voice.name} (${voice.gender})",
                                color = if (isSelected) JarvisCyan else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = voice.description,
                                color = JarvisTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Continuous Calling Auto-Listen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Calling Mode Continuous Loop",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Listen → Think → Speak → Auto-Listen again continuously.",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = callingModeAutoListen,
                        onCheckedChange = {
                            callingModeAutoListen = it
                            prefs.setCallingModeAutoListen(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan, checkedTrackColor = JarvisSurfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Barge-in Interruption
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Barge-in / Voice Interruption",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Interrupt JARVIS speech instantly if user speaks or says 'Stop'.",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = bargeInEnabled,
                        onCheckedChange = {
                            bargeInEnabled = it
                            prefs.setBargeInEnabled(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan, checkedTrackColor = JarvisSurfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Speech Rate Slider
                Text(
                    text = "Speech Rate (${String.format(Locale.US, "%.1fx", speechRate)})",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = speechRate,
                    onValueChange = {
                        speechRate = it
                        prefs.setSpeechRate(it)
                    },
                    valueRange = 0.8f..1.4f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 3. WAKE WORD & PUSH-TO-TALK (PTT) TRIGGERS ---
        SettingsSectionHeader(title = "VOICE INPUT & MICROPHONE TRIGGERS", icon = Icons.Default.Mic)

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Primary Microphone Trigger Mode",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Select how you initiate conversation with the Gemini AI service.",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                val triggerModes = listOf(
                    Triple("BOTH", "Both Active", "Wake Word 'JARVIS' + Push-to-Talk button"),
                    Triple("PTT", "Push-to-Talk Only", "Hold or tap button to speak (Manual Trigger)"),
                    Triple("WAKE_WORD", "Wake Word Only", "Hands-free continuous hotword recognition")
                )

                for ((modeKey, modeTitle, modeDesc) in triggerModes) {
                    val isSelected = triggerMode.equals(modeKey, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) JarvisCyan.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (isSelected) JarvisCyan else JarvisSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                triggerMode = modeKey
                                viewModel.setTriggerMode(modeKey)
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = modeTitle,
                                color = if (isSelected) JarvisCyan else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = modeDesc,
                                color = JarvisTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wake Word Hotword Engine",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Listen for wake phrase using Google Speech Recognizer.",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = {
                            wakeWordEnabled = it
                            prefs.setWakeWordEnabled(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan, checkedTrackColor = JarvisSurfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Wake Phrase",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val phrases = listOf("JARVIS", "Hey JARVIS")
                    for (phrase in phrases) {
                        val isSelected = wakePhrase.equals(phrase, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) JarvisCyan.copy(alpha = 0.2f) else JarvisSurfaceVariant)
                                .border(1.dp, if (isSelected) JarvisCyan else JarvisSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    wakePhrase = phrase
                                    prefs.setWakePhrase(phrase)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = phrase,
                                color = if (isSelected) JarvisCyan else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 4. PERSONALITY MODES ---
        SettingsSectionHeader(title = "ASSISTANT PERSONALITY", icon = Icons.Default.Psychology)

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                for (p in Personality.entries) {
                    val isSelected = personalityMode.equals(p.name, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) JarvisCyan.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (isSelected) JarvisCyan else JarvisSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                personalityMode = p.name
                                prefs.setPersonality(p.name)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = p.title.uppercase(),
                                color = if (isSelected) JarvisCyan else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = p.description,
                                color = JarvisTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 5. SECURITY & CONFIRMATIONS ---
        SettingsSectionHeader(title = "SECURITY & SAFEGUARDS", icon = Icons.Default.Security)

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Require Confirmation for Sensitive Actions",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Always request user authorization before placing phone calls, sending SMS, or deleting data.",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = confirmSensitiveActions,
                        onCheckedChange = {
                            confirmSensitiveActions = it
                            prefs.setConfirmSensitiveActions(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan, checkedTrackColor = JarvisSurfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "• API keys are never exposed in UI or logs.\n• No unauthorized background actions occur without verification.",
                    color = JarvisTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = JarvisTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}
