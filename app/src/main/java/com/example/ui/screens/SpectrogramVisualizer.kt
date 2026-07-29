package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SpectrogramVisualizer(
    isListening: Boolean,
    rmsDb: Float,
    modifier: Modifier = Modifier
) {
    // Smooth the RMS db input
    val normalizedRms = if (isListening) {
        ((rmsDb + 2f) / 14f).coerceIn(0.15f, 1.0f)
    } else {
        0.05f
    }

    val animatedRms by animateFloatAsState(
        targetValue = normalizedRms,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "rmsAnimation"
    )

    // Continuous time phase for smooth dancing waves
    val infiniteTransition = rememberInfiniteTransition(label = "spectrogramWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0B1329).copy(alpha = 0.85f))
            .border(1.dp, Brush.linearGradient(
                listOf(
                    if (isListening) Color(0xFF6366F1) else Color.White.copy(alpha = 0.15f),
                    if (isListening) Color(0xFFEC4899) else Color.White.copy(alpha = 0.05f)
                )
            ), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Status & Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isListening) Color(0xFF22C55E) else Color(0xFF94A3B8))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isListening) "VOICE CAPTURE ACTIVE" else "AUDIO STANDBY",
                        color = if (isListening) Color(0xFF86EFAC) else Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }

                Text(
                    text = if (isListening) "${(animatedRms * 28).toInt()} dB • 44.1 kHz" else "0 dB • PCM",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Spectrogram & Oscilloscope Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val width = size.width
                val height = size.height
                val centerY = height / 2f

                // Draw background grid lines
                val gridLines = 4
                for (i in 1..gridLines) {
                    val y = height * (i.toFloat() / (gridLines + 1))
                    drawLine(
                        color = Color.White.copy(alpha = 0.04f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 1. Draw Spectrogram Frequency Bars
                val barCount = 28
                val barSpacing = 3.dp.toPx()
                val totalSpacing = barSpacing * (barCount - 1)
                val barWidth = (width - totalSpacing) / barCount

                val barBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF43F5E), // Rose peak
                        Color(0xFFC084FC), // Purple mid
                        Color(0xFF38BDF8), // Cyan base
                        Color(0xFF6366F1)  // Indigo bottom
                    )
                )

                for (i in 0 until barCount) {
                    val x = i * (barWidth + barSpacing)
                    
                    // Frequency multiplier curve
                    val freqFactor = sin((i.toDouble() / barCount) * PI).toFloat()
                    
                    // Wave variation per bar
                    val barPhase = phase + (i * 0.35f)
                    val waveValue = (sin(barPhase.toDouble()) + 1.0).toFloat() / 2f
                    
                    // Calculate bar height based on audio level
                    val minBarHeight = 4.dp.toPx()
                    val maxDynamicHeight = (height * 0.85f) * freqFactor * animatedRms
                    val barHeight = (minBarHeight + (waveValue * maxDynamicHeight)).coerceAtMost(height)

                    val top = centerY - (barHeight / 2f)

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }

                // 2. Draw Real-time Oscilloscope Waveform Path
                val wavePath = Path()
                val wavePoints = 60
                val stepX = width / wavePoints

                for (i in 0..wavePoints) {
                    val x = i * stepX
                    val normalizedX = i.toFloat() / wavePoints
                    
                    val envelope = sin(normalizedX.toDouble() * PI).toFloat()
                    
                    val wave1 = sin((phase * 2f + normalizedX * 4 * PI).toDouble()).toFloat()
                    val wave2 = sin((-phase + normalizedX * 8 * PI).toDouble()).toFloat() * 0.5f
                    
                    val y = centerY + (wave1 + wave2) * (height * 0.35f) * animatedRms * envelope

                    if (i == 0) {
                        wavePath.moveTo(x, y)
                    } else {
                        wavePath.lineTo(x, y)
                    }
                }

                // Wave Glow Effect
                drawPath(
                    path = wavePath,
                    color = Color(0xFF60A5FA).copy(alpha = 0.4f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
                // Crisp Core Wave Line
                drawPath(
                    path = wavePath,
                    color = Color.White,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}
