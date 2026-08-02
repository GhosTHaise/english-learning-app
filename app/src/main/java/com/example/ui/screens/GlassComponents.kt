package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
fun BackgroundOrbs() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgColor = MaterialTheme.colorScheme.background
    val orb1Color = if (isDark) Color(0xFF4F46E5).copy(alpha = 0.3f) else Color(0xFF818CF8).copy(alpha = 0.2f)
    val orb2Color = if (isDark) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFF60A5FA).copy(alpha = 0.15f)
    val orb3Color = if (isDark) Color(0xFF9333EA).copy(alpha = 0.2f) else Color(0xFFC084FC).copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-Left Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb1Color, Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = width * 0.6f
                ),
                center = Offset(0f, 0f),
                radius = width * 0.6f
            )

            // Right Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb2Color, Color.Transparent),
                    center = Offset(width, height * 0.5f),
                    radius = width * 0.7f
                ),
                center = Offset(width, height * 0.5f),
                radius = width * 0.7f
            )

            // Bottom-Left Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb3Color, Color.Transparent),
                    center = Offset(width * 0.2f, height * 0.9f),
                    radius = width * 0.5f
                ),
                center = Offset(width * 0.2f, height * 0.9f),
                radius = width * 0.5f
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    contentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(cardBg)
            .border(1.dp, cardBorder, shape)
            .padding(contentPadding)
    ) {
        content()
    }
}

