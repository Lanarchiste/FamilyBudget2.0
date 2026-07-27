package com.example.familybudget20.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaymentTile(
    name: String,
    amount: Double,
    paid: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (paid) Color(0xFF0F2F0F) else Color(0xFF1A1D24)
    val glowColor = if (paid) Color(0x5522FF22) else Color(0x552196F3)

    val scale by animateFloatAsState(
        targetValue = if (paid) 0.97f else 1f,
        label = "tileScale"
    )

    Box(
        modifier = modifier
            .height(110.dp)
            .scale(scale)
            .shadow(                          // ← shadow EN PREMIER
                elevation = 18.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .background(bgColor, RoundedCornerShape(16.dp))  // ← background APRÈS
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = name,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (paid) "✓" else "${String.format("%.2f", amount)} €",   // ← plus de "à verser"
                color = if (paid) Color(0xFF4CAF50) else Color.White,
                fontSize = 24.sp
            )
        }
    }
}


