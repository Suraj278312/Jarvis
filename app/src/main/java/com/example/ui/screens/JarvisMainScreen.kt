package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisState
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.StatusBadge
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceBorder
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.viewmodel.JarvisUiState
import com.example.viewmodel.JarvisViewModel

@Composable
fun JarvisMainScreen(
    viewModel: JarvisViewModel,
    uiState: JarvisUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- Top HUD Navigation Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (uiState.hasApiKey) JarvisGreen else JarvisAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "J.A.R.V.I.S.",
                        color = JarvisCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    text = if (uiState.isLiveWsConnected) "GEMINI LIVE • ONLINE" else "GEMINI BRAIN • READY",
                    color = JarvisTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Speaker toggle
                IconButton(
                    onClick = { viewModel.toggleSpeaker() },
                    modifier = Modifier.testTag("speaker_toggle_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isSpeakerActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Speaker Toggle",
                        tint = if (uiState.isSpeakerActive) JarvisCyan else JarvisTextMuted
                    )
                }

                // Mic mute toggle
                IconButton(
                    onClick = { viewModel.toggleMicMute() },
                    modifier = Modifier.testTag("mic_toggle_button")
                ) {
                    Icon(
                        imageVector = if (!uiState.isMicMuted) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Mic Toggle",
                        tint = if (!uiState.isMicMuted) JarvisCyan else JarvisRed
                    )
                }

                // Settings
                IconButton(
                    onClick = { viewModel.navigateTo("SETTINGS") },
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = JarvisCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Accessibility Service Reminder Banner
        if (!uiState.isAccessibilityActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2836)),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAmber.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = JarvisAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Accessibility Automation Inactive",
                            color = JarvisAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap to enable JARVIS screen reading & UI click actions in Settings.",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // --- Central Arc Reactor Visualizer ---
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .clip(CircleShape)
                .clickable {
                    // Tap reactor triggers immediate voice directive
                    viewModel.triggerOneShotVoiceListen()
                }
                .testTag("central_arc_reactor"),
            contentAlignment = Alignment.Center
        ) {
            ArcReactorVisualizer(
                state = uiState.jarvisState,
                amplitude = uiState.audioAmplitude,
                diameter = 200.dp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // State Badge
        StatusBadge(
            state = uiState.jarvisState,
            statusText = uiState.statusMessage
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Waveform Equalizer
        WaveformVisualizer(
            amplitude = uiState.audioAmplitude,
            barCount = 20,
            maxHeight = 32.dp,
            barColor = when (uiState.jarvisState) {
                JarvisState.LISTENING -> JarvisCyan
                JarvisState.THINKING -> JarvisAmber
                JarvisState.SPEAKING -> JarvisElectricBlue
                JarvisState.INTERRUPTED -> JarvisRed
                else -> JarvisCyan.copy(alpha = 0.5f)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- Push-to-Talk & Wake-Word Trigger Control ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (uiState.isPttPressed || uiState.jarvisState == JarvisState.LISTENING) JarvisCyan else JarvisSurfaceBorder,
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isPttPressed) Color(0xFF072138) else JarvisSurface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trigger Mode Switcher Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VOICE INPUT TRIGGER",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TriggerModeChip(
                            label = "BOTH",
                            isSelected = uiState.triggerMode == "BOTH",
                            onClick = { viewModel.setTriggerMode("BOTH") }
                        )
                        TriggerModeChip(
                            label = "PTT",
                            isSelected = uiState.triggerMode == "PTT",
                            onClick = { viewModel.setTriggerMode("PTT") }
                        )
                        TriggerModeChip(
                            label = "WAKE WORD",
                            isSelected = uiState.triggerMode == "WAKE_WORD",
                            onClick = { viewModel.setTriggerMode("WAKE_WORD") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The Big Tactile Push-to-Talk Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (uiState.isPttPressed) Brush.horizontalGradient(listOf(JarvisCyan, JarvisElectricBlue))
                            else Brush.horizontalGradient(listOf(Color(0xFF0E2238), Color(0xFF091626)))
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (uiState.isPttPressed) Color.White else JarvisCyan.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.startPushToTalk()
                                    tryAwaitRelease()
                                    viewModel.stopPushToTalk()
                                },
                                onTap = {
                                    viewModel.toggleTapToTalk()
                                }
                            )
                        }
                        .testTag("push_to_talk_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isPttPressed) Icons.Default.Mic else Icons.Default.Hearing,
                            contentDescription = "Push to Talk",
                            tint = if (uiState.isPttPressed) JarvisBackground else JarvisCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (uiState.isPttPressed) "RELEASE TO SEND TO GEMINI" else "HOLD OR TAP TO TALK",
                            color = if (uiState.isPttPressed) JarvisBackground else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val hintText = when (uiState.triggerMode) {
                    "PTT" -> "Push-to-Talk active • Press & hold button to speak"
                    "WAKE_WORD" -> "Wake Word active • Say 'JARVIS' or 'Hey JARVIS' anytime"
                    else -> "Say 'JARVIS' hands-free or Hold button for Push-to-Talk"
                }

                Text(
                    text = hintText,
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Live HUD Information Terminal ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TERMINAL FEED",
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    if (uiState.liveTranscript.isNotBlank()) {
                        Text(
                            text = "VOICE INPUT DETECTED",
                            color = JarvisAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val displayText = if (uiState.liveTranscript.isNotBlank()) {
                    "\"${uiState.liveTranscript}\""
                } else {
                    uiState.latestAiResponse
                }

                Text(
                    text = displayText,
                    color = JarvisTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Instant Interruption Button (when speaking or listening) ---
        AnimatedVisibility(
            visible = uiState.jarvisState == JarvisState.SPEAKING || uiState.jarvisState == JarvisState.LISTENING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisRed.copy(alpha = 0.2f))
                    .border(1.dp, JarvisRed, RoundedCornerShape(12.dp))
                    .clickable { viewModel.stopAssistant() }
                    .padding(vertical = 12.dp)
                    .testTag("instant_stop_button"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = JarvisRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "JARVIS, STOP",
                    color = JarvisRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // --- Primary Action Grid ---
        Text(
            text = "TACTICAL DIRECTIVES",
            color = JarvisTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Big Calling Mode Trigger
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { viewModel.startCallingMode() }
                .border(1.dp, JarvisCyan.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .testTag("enter_calling_mode_button"),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0F2B48), Color(0xFF0C1929))
                        )
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan.copy(alpha = 0.15f))
                        .border(1.dp, JarvisCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Calling Mode",
                        tint = JarvisCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ENGAGE CALLING MODE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Continuous hands-free conversation with Gemini Live audio brain.",
                        color = JarvisCyanLight,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Action Row (Chat, Notes, Automation)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Chat Terminal
            ActionTile(
                title = "CHAT",
                subtitle = "Text Brain",
                icon = Icons.Default.Chat,
                iconColor = JarvisElectricBlue,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_chat_button"),
                onClick = { viewModel.navigateTo("CHAT") }
            )

            // Notes & Memory
            ActionTile(
                title = "MEMORY",
                subtitle = "Notes & Facts",
                icon = Icons.Default.FolderShared,
                iconColor = JarvisAmber,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_memory_button"),
                onClick = { viewModel.navigateTo("NOTES_MEMORY") }
            )

            // Automation & Screen
            ActionTile(
                title = "CONTROL",
                subtitle = "Screen & Phone",
                icon = Icons.Default.PhoneAndroid,
                iconColor = JarvisCyan,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_automation_button"),
                onClick = { viewModel.navigateTo("AUTOMATION") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = JarvisTextMuted,
                fontSize = 9.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TriggerModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) JarvisCyan.copy(alpha = 0.25f) else JarvisSurfaceVariant)
            .border(
                1.dp,
                if (isSelected) JarvisCyan else JarvisSurfaceBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) JarvisCyan else JarvisTextMuted,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}

