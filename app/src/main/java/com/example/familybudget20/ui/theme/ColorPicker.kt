package com.example.familybudget20.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ColorPicker(
    selected: String,
    onSelect: (String) -> Unit
) {
    val colors = listOf(
        "#2196F3", "#4CAF50", "#FF9800", "#F44336",
        "#9C27B0", "#00BCD4", "#E91E63", "#795548"
    )

    val scroll = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 24.dp), // ← centre visuellement
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { hex ->
            val color = Color(android.graphics.Color.parseColor(hex))

            Box(
                modifier = Modifier
                    .size(48.dp) // ← un peu plus grand = plus joli
                    .background(color, CircleShape)
                    .border(
                        width = if (hex == selected) 4.dp else 2.dp,
                        color = if (hex == selected) Color.White else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}
