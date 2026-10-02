package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.JarvisState
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    state: JarvisState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorRotation")

    // Continuous rotation angles
    val angleClockwise by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "clockwise"
    )

    val angleCounterClockwise by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_clockwise"
    )

    // Breathing pulse for standby
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val baseColor = when (state) {
        JarvisState.STANDBY -> JarvisCyan
        JarvisState.LISTENING -> JarvisCyan
        JarvisState.THINKING -> JarvisAmber
        JarvisState.SPEAKING -> JarvisElectricBlue
        JarvisState.INTERRUPTED -> JarvisRed
        JarvisState.CONNECTING -> JarvisAmber
        JarvisState.CONNECTED -> JarvisCyan
        JarvisState.ERROR -> JarvisRed
    }

    val effectivePulse = if (state == JarvisState.STANDBY) {
        breathingPulse
    } else {
        1.0f + (amplitude.coerceIn(0f, 1f) * 0.4f)
    }

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = (size.width.coerceAtMost(size.height) / 2f) * 0.9f

            // 1. Ambient Background Core Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.35f * effectivePulse.coerceAtMost(1.5f)),
                        baseColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // 2. Outer Fixed Boundary Tech Ring
            drawCircle(
                color = baseColor.copy(alpha = 0.25f),
                radius = maxRadius * 0.95f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 3. Rotating Outer Segmented Arcs
            rotate(angleClockwise, pivot = center) {
                val outerArcRadius = maxRadius * 0.88f
                val sweep = 45f
                for (i in 0 until 4) {
                    val startAngle = i * 90f + 15f
                    drawArc(
                        color = baseColor.copy(alpha = 0.7f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - outerArcRadius, center.y - outerArcRadius),
                        size = androidx.compose.ui.geometry.Size(outerArcRadius * 2, outerArcRadius * 2),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 4. Middle Counter-Rotating Gear Ring
            rotate(angleCounterClockwise, pivot = center) {
                val midRadius = maxRadius * 0.68f
                drawCircle(
                    color = baseColor.copy(alpha = 0.3f),
                    radius = midRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // High-tech tick marks
                val ticks = 12
                for (i in 0 until ticks) {
                    val angle = Math.toRadians((i * (360.0 / ticks)))
                    val r1 = midRadius - 8.dp.toPx()
                    val r2 = midRadius + 8.dp.toPx()
                    val start = Offset(
                        center.x + (r1 * cos(angle)).toFloat(),
                        center.y + (r1 * sin(angle)).toFloat()
                    )
                    val end = Offset(
                        center.x + (r2 * cos(angle)).toFloat(),
                        center.y + (r2 * sin(angle)).toFloat()
                    )
                    drawLine(
                        color = baseColor.copy(alpha = 0.85f),
                        start = start,
                        end = end,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 5. Pulsating Inner Reactor Core
            val coreRadius = (maxRadius * 0.40f) * effectivePulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        baseColor.copy(alpha = 0.85f),
                        baseColor.copy(alpha = 0.2f)
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // Inner Core Boundary
            drawCircle(
                color = Color.White,
                radius = coreRadius * 0.45f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
