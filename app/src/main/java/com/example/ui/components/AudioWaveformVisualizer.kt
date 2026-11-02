package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.VoiceState
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import kotlin.math.sin

@Composable
fun AudioWaveformVisualizer(
    voiceState: VoiceState,
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_anim"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val barCount = 28
        val totalSpacing = size.width / (barCount + 1)
        val barWidth = totalSpacing * 0.55f
        val centerY = size.height / 2f

        val active = voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING || voiceState == VoiceState.THINKING

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount.toFloat()
            val wave = if (active) {
                val freq = 2.5f
                val dynamicAmp = if (voiceState == VoiceState.LISTENING) {
                    0.25f + (audioRms * 0.75f)
                } else if (voiceState == VoiceState.SPEAKING) {
                    0.4f + (sin(phase + i * 0.4f) * 0.3f)
                } else {
                    0.2f + (sin(phase * 1.5f + i * 0.3f) * 0.15f)
                }
                val envelope = sin(normalizedX * Math.PI).toFloat() // taper at edges
                (0.15f + dynamicAmp * envelope * 0.85f).coerceIn(0.1f, 1f)
            } else {
                0.08f
            }

            val barHeight = size.height * wave
            val x = (i + 1) * totalSpacing - (barWidth / 2f)
            val y = centerY - (barHeight / 2f)

            val colorBrush = Brush.verticalGradient(
                colors = listOf(
                    NeonCyan,
                    NeonPurple,
                    NeonBlue
                ),
                startY = y,
                endY = y + barHeight
            )

            drawRoundRect(
                brush = colorBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
