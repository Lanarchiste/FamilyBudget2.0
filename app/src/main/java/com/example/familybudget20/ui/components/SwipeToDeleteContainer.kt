package com.example.familybudget20.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    remainingAmount: Double = 0.0,
    onDeleteConfirmed: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var rowHeight by remember { mutableStateOf(0) }

    val contentAlpha by animateFloatAsState(
        targetValue = if (confirmDelete) 0f else 1f,
        label = "contentAlpha"
    )

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                confirmDelete = true
                false
            } else false
        }
    )

    Box(modifier = Modifier.fillMaxWidth()) {

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {},
            content = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(contentAlpha)
                        .onSizeChanged { rowHeight = it.height },
                    content = content
                )
            }
        )

        if (confirmDelete) {
            val heightDp = with(LocalDensity.current) { rowHeight.toDp() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Supprimer ?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Action irréversible", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2A2A))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { confirmDelete = false }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Annuler", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE53935))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                confirmDelete = false
                                if (remainingAmount > 0.0) {
                                    showBlockedDialog = true
                                } else {
                                    onDeleteConfirmed()
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Supprimer", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showBlockedDialog) {
            AlertDialog(
                onDismissRequest = { showBlockedDialog = false },
                containerColor = Color(0xFF1A1D24),
                title = { Text("Suppression impossible", color = Color.White) },
                text = {
                    Text(
                        "Il reste ${String.format("%.2f", remainingAmount)} € sur cette ligne. Remets le solde à 0 avant de la supprimer.",
                        color = Color.Gray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showBlockedDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) { Text("Compris", color = Color.White) }
                }
            )
        }
    }
}