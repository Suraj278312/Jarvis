package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisState
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurfaceVariant

@Composable
fun StatusBadge(
    state: JarvisState,
    statusText: String? = null,
    modifier: Modifier = Modifier
) {
    val stateColor = when (state) {
        JarvisState.STANDBY -> JarvisCyan
        JarvisState.LISTENING -> JarvisCyan
        JarvisState.THINKING -> JarvisAmber
        JarvisState.SPEAKING -> JarvisElectricBlue
        JarvisState.INTERRUPTED -> JarvisRed
        JarvisState.CONNECTING -> JarvisAmber
        JarvisState.CONNECTED -> JarvisGreen
        JarvisState.ERROR -> JarvisRed
    }

    val infiniteTransition = rememberInfiniteTransition(label = "StatusGlow")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusAlpha"
    )

    val label = statusText ?: when (state) {
        JarvisState.STANDBY -> "STANDBY • AWAITING WAKE WORD"
        JarvisState.LISTENING -> "LISTENING • RECORDING SPEECH"
        JarvisState.THINKING -> "THINKING • PROCESSING DIRECTIVE"
        JarvisState.SPEAKING -> "SPEAKING • AUDIO ACTIVE"
        JarvisState.INTERRUPTED -> "INTERRUPTED • BARGE-IN DETECTED"
        JarvisState.CONNECTING -> "CONNECTING • ESTABLISHING GEMINI"
        JarvisState.CONNECTED -> "CONNECTED • GEMINI LIVE ONLINE"
        JarvisState.ERROR -> "SYSTEM ERROR • RETRY DIRECTIVE"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(JarvisSurfaceVariant.copy(alpha = 0.85f))
            .border(1.dp, stateColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glowing LED Indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(stateColor.copy(alpha = if (state == JarvisState.STANDBY) 0.6f else alphaAnim))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}
