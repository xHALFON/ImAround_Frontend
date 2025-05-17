package com.example.myapplication.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfilePhotoAnalysisTip(
    tip: String,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    // Animation effect
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize()
            .scale(cardScale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6)
                            )
                        )
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated lightbulb icon
                    val infiniteTransition = rememberInfiniteTransition(label = "bulb_animation")
                    val iconScale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(700, easing = EaseInOutQuad),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "icon_scale"
                    )

                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color.Yellow,
                        modifier = Modifier
                            .size(28.dp)
                            .scale(iconScale)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "AI Photo Tip",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF6366F1),
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Analyzing your photo...",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                Text(
                    text = tip,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dismiss button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                )
            ) {
                Text("Got it")
            }
        }
    }
}