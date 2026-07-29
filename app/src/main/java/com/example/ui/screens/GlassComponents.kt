package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun BackgroundOrbs() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-Left Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF4F46E5).copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = width * 0.6f
                ),
                center = Offset(0f, 0f),
                radius = width * 0.6f
            )

            // Right Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(width, height * 0.5f),
                    radius = width * 0.7f
                ),
                center = Offset(width, height * 0.5f),
                radius = width * 0.7f
            )

            // Bottom-Left Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF9333EA).copy(alpha = 0.2f), Color.Transparent),
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
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), shape)
            .padding(24.dp)
    ) {
        content()
    }
}
