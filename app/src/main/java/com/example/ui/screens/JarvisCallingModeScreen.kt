package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisState
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.StatusBadge
import com.example.ui.components.WaveformVisualizer
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
import com.example.viewmodel.JarvisUiState
import com.example.viewmodel.JarvisViewModel

@Composable
fun JarvisCallingModeScreen(
    viewModel: JarvisViewModel,
    uiState: JarvisUiState,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.exitCallingMode()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF030712), Color(0xFF070E1E), Color(0xFF030712))
                )
            )
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Status & Close Row
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
                            .background(if (uiState.isLiveWsConnected) JarvisGreen else JarvisCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CALLING MODE ENGAGED",
                        color = JarvisCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = if (uiState.isLiveWsConnected) "GEMINI LIVE PROTOCOL ACTIVE" else "CONTINUOUS AUDIO LOOP ACTIVE",
                    color = JarvisTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Connection indicator pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurfaceVariant)
                    .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (uiState.isLiveWsConnected) "LIVE WS" else "LOCAL DUPLEX",
                    color = if (uiState.isLiveWsConnected) JarvisGreen else JarvisAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- Continuous Conversation State Pipeline Tracker ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepIndicator(
                    label = "LISTEN",
                    icon = Icons.Default.Hearing,
                    isActive = uiState.jarvisState == JarvisState.LISTENING,
                    activeColor = JarvisCyan
                )

                Text(text = "→", color = JarvisTextMuted, fontSize = 14.sp)

                StepIndicator(
                    label = "THINK",
                    icon = Icons.Default.Psychology,
                    isActive = uiState.jarvisState == JarvisState.THINKING,
                    activeColor = JarvisAmber
                )

                Text(text = "→", color = JarvisTextMuted, fontSize = 14.sp)

                StepIndicator(
                    label = "SPEAK",
                    icon = Icons.Default.RecordVoiceOver,
                    isActive = uiState.jarvisState == JarvisState.SPEAKING,
                    activeColor = JarvisElectricBlue
                )

                Text(text = "↺", color = JarvisCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Immersive Calling Core Visualizer ---
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            ArcReactorVisualizer(
                state = uiState.jarvisState,
                amplitude = uiState.audioAmplitude,
                diameter = 230.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        StatusBadge(
            state = uiState.jarvisState,
            statusText = when (uiState.jarvisState) {
                JarvisState.LISTENING -> "HEARING YOUR VOICE..."
                JarvisState.THINKING -> "GEMINI IS PROCESSING..."
                JarvisState.SPEAKING -> "JARVIS IS SPEAKING (BARGE-IN READY)"
                JarvisState.INTERRUPTED -> "INTERRUPTED BY USER"
                else -> "AWAITING NEXT STATEMENT"
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Frequency Waveform
        WaveformVisualizer(
            amplitude = uiState.audioAmplitude,
            barCount = 28,
            maxHeight = 36.dp,
            barColor = when (uiState.jarvisState) {
                JarvisState.LISTENING -> JarvisCyan
                JarvisState.THINKING -> JarvisAmber
                JarvisState.SPEAKING -> JarvisElectricBlue
                else -> JarvisCyan.copy(alpha = 0.6f)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Live Transcript Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "LIVE SPEECH TRANSCRIPTION",
                    color = JarvisTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val liveText = if (uiState.liveTranscript.isNotBlank()) {
                    "User: \"${uiState.liveTranscript}\""
                } else if (uiState.latestAiResponse.isNotBlank()) {
                    "JARVIS: \"${uiState.latestAiResponse}\""
                } else {
                    "Listening for continuous speech... Speak freely without pressing buttons."
                }

                Text(
                    text = liveText,
                    color = JarvisTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Calling Mode Interactive Controls ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute / Unmute
            CallingControlPill(
                icon = if (!uiState.isMicMuted) Icons.Default.Mic else Icons.Default.MicOff,
                label = if (!uiState.isMicMuted) "Mute" else "Unmute",
                tint = if (!uiState.isMicMuted) JarvisCyan else JarvisRed,
                onClick = { viewModel.toggleMicMute() },
                testTag = "calling_mic_toggle"
            )

            // Barge-In / Stop Button
            CallingControlPill(
                icon = Icons.Default.Stop,
                label = "Interrupt",
                tint = JarvisAmber,
                onClick = { viewModel.stopAssistant() },
                testTag = "calling_interrupt_button"
            )

            // Speaker Toggle
            CallingControlPill(
                icon = if (uiState.isSpeakerActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                label = if (uiState.isSpeakerActive) "Speaker" else "Muted",
                tint = if (uiState.isSpeakerActive) JarvisCyan else JarvisTextMuted,
                onClick = { viewModel.toggleSpeaker() },
                testTag = "calling_speaker_toggle"
            )

            // End Call Button
            CallingControlPill(
                icon = Icons.Default.CallEnd,
                label = "End",
                tint = Color.White,
                backgroundColor = JarvisRed,
                onClick = { viewModel.exitCallingMode() },
                testTag = "end_calling_mode_button"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StepIndicator(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) activeColor.copy(alpha = 0.25f) else Color.Transparent,
        animationSpec = tween(300),
        label = "stepBg"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else JarvisTextMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isActive) activeColor else JarvisTextMuted,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun CallingControlPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    backgroundColor: Color = JarvisSurfaceVariant,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(1.dp, JarvisSurfaceBorder, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = JarvisTextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
