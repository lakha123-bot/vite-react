package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.VoiceState
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowPurple
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicJarvisOrb(
    state: VoiceState,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb_trans")

    // Slow continuous outer rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == VoiceState.THINKING) 3000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    // Counter rotation for inner ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == VoiceState.THINKING) 2000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    VoiceState.LISTENING -> 800
                    VoiceState.SPEAKING -> 600
                    VoiceState.THINKING -> 500
                    VoiceState.ERROR -> 400
                    else -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Wave ripple expansion
    val waveRipple by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_ripple"
    )

    val (primaryGlow, secondaryGlow) = when (state) {
        VoiceState.LISTENING -> Pair(NeonCyan, NeonBlue)
        VoiceState.THINKING -> Pair(NeonPurple, NeonCyan)
        VoiceState.SPEAKING -> Pair(NeonPurple, NeonBlue)
        VoiceState.ERROR -> Pair(NeonMagenta, NeonAmber)
        VoiceState.IDLE -> Pair(NeonCyan, NeonPurple)
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("holographic_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.75f
            val dynamicScale = breathingPulse + (audioRms * 0.25f)
            val effectiveRadius = baseRadius * dynamicScale

            // 1. Outermost audio/energy reactive ripple (when speaking or listening)
            if (state == VoiceState.LISTENING || state == VoiceState.SPEAKING) {
                val rippleRadius = baseRadius * waveRipple
                val rippleAlpha = (1f - (waveRipple - 0.5f) / 0.9f).coerceIn(0f, 0.6f)
                drawCircle(
                    color = primaryGlow.copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 2. Large diffused radial glow behind the orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.35f),
                        secondaryGlow.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = effectiveRadius * 1.5f
                ),
                radius = effectiveRadius * 1.5f,
                center = center
            )

            // 3. Outer Sci-Fi Mechanical Ring with Notch Ticks
            rotate(outerRotation, pivot = center) {
                drawCircle(
                    color = primaryGlow.copy(alpha = 0.4f),
                    radius = effectiveRadius * 1.05f,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Tick marks along the outer perimeter
                val tickCount = 24
                for (i in 0 until tickCount) {
                    val angle = (i * (360f / tickCount)) * (PI / 180f).toFloat()
                    val r1 = effectiveRadius * 1.02f
                    val r2 = if (i % 6 == 0) effectiveRadius * 1.15f else effectiveRadius * 1.08f
                    val start = Offset(center.x + r1 * cos(angle), center.y + r1 * sin(angle))
                    val end = Offset(center.x + r2 * cos(angle), center.y + r2 * sin(angle))
                    drawLine(
                        color = if (i % 6 == 0) primaryGlow else secondaryGlow.copy(alpha = 0.5f),
                        start = start,
                        end = end,
                        strokeWidth = if (i % 6 == 0) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Four curved quantum arc segments
                for (arcStart in listOf(15f, 105f, 195f, 285f)) {
                    drawArc(
                        color = primaryGlow,
                        startAngle = arcStart,
                        sweepAngle = 60f,
                        useCenter = false,
                        topLeft = Offset(center.x - effectiveRadius * 1.1f, center.y - effectiveRadius * 1.1f),
                        size = androidx.compose.ui.geometry.Size(effectiveRadius * 2.2f, effectiveRadius * 2.2f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 4. Counter-rotating Intermediate Cyber Ring with Orbital Particle Nodes
            rotate(innerRotation, pivot = center) {
                drawCircle(
                    color = secondaryGlow.copy(alpha = 0.35f),
                    radius = effectiveRadius * 0.85f,
                    center = center,
                    style = Stroke(
                        width = 1.5.dp.toPx()
                    )
                )

                // Orbital nodes (dots)
                val nodeAngles = listOf(0f, 60f, 120f, 180f, 240f, 300f)
                for (deg in nodeAngles) {
                    val rad = deg * (PI / 180f).toFloat()
                    val nodePos = Offset(
                        center.x + effectiveRadius * 0.85f * cos(rad),
                        center.y + effectiveRadius * 0.85f * sin(rad)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = nodePos
                    )
                    drawCircle(
                        color = primaryGlow,
                        radius = 6.dp.toPx(),
                        center = nodePos,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // 5. Plasma Arc Reactor Inner Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryGlow,
                        secondaryGlow,
                        Color.Transparent
                    ),
                    center = center,
                    radius = effectiveRadius * 0.65f
                ),
                radius = effectiveRadius * 0.65f,
                center = center
            )

            // Inner core bounding ring with glow
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = effectiveRadius * 0.35f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Dynamic core center iris
            val irisRadius = (effectiveRadius * 0.18f) * (1f + audioRms * 0.6f)
            drawCircle(
                color = primaryGlow,
                radius = irisRadius,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = irisRadius * 0.45f,
                center = center
            )
        }
    }
}
