package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import kotlin.random.Random

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    barCount: Int = 18,
    maxHeight: Dp = 48.dp,
    barColor: Color = JarvisCyan,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val rand = Random(42)
        for (i in 0 until barCount) {
            val distFromCenter = Math.abs(i - (barCount / 2f)) / (barCount / 2f)
            val bellFactor = (1.0f - (distFromCenter * 0.5f)).coerceAtLeast(0.2f)
            val jitter = 0.7f + (rand.nextFloat() * 0.6f)
            val effectiveHeight = (amplitude * bellFactor * jitter).coerceIn(0.12f, 1.0f)

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(effectiveHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor.copy(alpha = 0.35f + (effectiveHeight * 0.65f)))
            )
        }
    }
}
