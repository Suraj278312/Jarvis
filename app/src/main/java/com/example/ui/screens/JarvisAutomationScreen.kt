package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScreenInfo
import com.example.service.JarvisAccessibilityService
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
fun JarvisAutomationScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo("MAIN")
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isA11yActive by viewModel.isAccessibilityEnabled.collectAsState()

    var inspectedScreenInfo by remember { mutableStateOf<ScreenInfo?>(null) }
    var actionStatusFeedback by remember { mutableStateOf<String?>(null) }

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
                modifier = Modifier.testTag("automation_back_button")
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
                    text = "DEVICE CONTROL & AUTOMATION",
                    color = JarvisCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Screen Inspection • Touch • System Actions",
                    color = JarvisTextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Accessibility Service Status Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isA11yActive) JarvisGreen.copy(alpha = 0.5f) else JarvisAmber.copy(alpha = 0.5f),
                    RoundedCornerShape(14.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isA11yActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isA11yActive) JarvisGreen else JarvisAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isA11yActive) "ACCESSIBILITY ACTIVE" else "ACCESSIBILITY DISABLED",
                            color = if (isA11yActive) JarvisGreen else JarvisAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isA11yActive) JarvisSurfaceVariant else JarvisAmber,
                            contentColor = if (isA11yActive) JarvisCyan else Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isA11yActive) "Configure" else "Enable Service",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Android Accessibility Service powers JARVIS screen understanding: reading UI text, locating buttons/fields, tapping elements, scrolling, and typing as requested in voice commands.",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Live Screen Reader Diagnostic ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE SCREEN INSPECTOR",
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Button(
                        onClick = {
                            val info = viewModel.automationEngine.readScreen()
                            inspectedScreenInfo = info
                            actionStatusFeedback = if (info != null) {
                                "Inspected ${info.nodes.size} nodes, found ${info.clickableElements.size} clickable elements."
                            } else {
                                "Accessibility Service is not active. Enable in Settings."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan,
                            contentColor = JarvisBackground
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FindInPage, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Inspect Screen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (inspectedScreenInfo != null) {
                    val info = inspectedScreenInfo!!
                    Text(
                        text = "Package: ${info.packageName ?: "Unknown"}\nElements Found: ${info.nodes.size}",
                        color = JarvisAmber,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Extracted Text Content:\n${info.readableText.take(400)}",
                        color = JarvisTextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = "Tap 'Inspect Screen' to evaluate what JARVIS sees on the active Android window.",
                        color = JarvisTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Status Banner
        if (actionStatusFeedback != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F263E))
                    .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "DIRECTIVE RESULT: $actionStatusFeedback",
                    color = JarvisCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Quick Direct Actions Test Panel ---
        Text(
            text = "HARDWARE & SYSTEM SHORTCUTS",
            color = JarvisTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutomationActionButton(
                label = "Open YouTube",
                icon = Icons.Default.AppShortcut,
                modifier = Modifier.weight(1f),
                onClick = {
                    actionStatusFeedback = viewModel.automationEngine.openApp("YouTube")
                }
            )
            AutomationActionButton(
                label = "Web Browser",
                icon = Icons.Default.AppShortcut,
                modifier = Modifier.weight(1f),
                onClick = {
                    actionStatusFeedback = viewModel.automationEngine.openApp("Chrome")
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutomationActionButton(
                label = "System Settings",
                icon = Icons.Default.Settings,
                modifier = Modifier.weight(1f),
                onClick = {
                    actionStatusFeedback = viewModel.automationEngine.openSettingsScreen("main")
                }
            )
            AutomationActionButton(
                label = "Raise Volume",
                icon = Icons.Default.VolumeUp,
                modifier = Modifier.weight(1f),
                onClick = {
                    actionStatusFeedback = viewModel.automationEngine.adjustVolume("up")
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutomationActionButton(
                label = "Global Home",
                icon = Icons.Default.Navigation,
                modifier = Modifier.weight(1f),
                onClick = {
                    val res = viewModel.automationEngine.navigateSystem("home")
                    actionStatusFeedback = if (res) "Navigated Home." else "Ensure Accessibility is ON."
                }
            )
            AutomationActionButton(
                label = "Global Back",
                icon = Icons.Default.Navigation,
                modifier = Modifier.weight(1f),
                onClick = {
                    val res = viewModel.automationEngine.navigateSystem("back")
                    actionStatusFeedback = if (res) "Navigated Back." else "Ensure Accessibility is ON."
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AutomationActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
