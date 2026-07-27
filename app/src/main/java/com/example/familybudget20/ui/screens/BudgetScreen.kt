package com.example.familybudget20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.familybudget20.model.BudgetLine
import com.example.familybudget20.ui.components.BudgetLineTile
import com.example.familybudget20.ui.components.SwipeToDeleteContainer
import com.example.familybudget20.viewmodel.StartupViewModel

@Composable
fun BudgetScreen(
    viewModel: StartupViewModel,
    modifier: Modifier = Modifier
) {
    val budgetLines = viewModel.budgetLines.collectAsState().value
    val activeMode = viewModel.activeMode.collectAsState().value
    var showAddSheet by remember { mutableStateOf(false) }
    var lineToEdit by remember { mutableStateOf<BudgetLine?>(null) }
    var showNextMonthConfirm by remember { mutableStateOf(false) }

    // Onglets selon le mode
    val tabs = if (activeMode == "solo") {
        listOf("current" to "🔄 Récurrente", "saving" to "💚 Épargne")
    } else {
        listOf("current" to "💙 Courant", "saving" to "💚 Épargne")
    }

    var selectedTab by remember { mutableStateOf("current") }
    val filteredLines = budgetLines.filter { it.type == selectedTab }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        ) {

        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { showAddSheet = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeMode == "solo") Color(0xFF7C4DFF) else Color(0xFF2196F3)
                )
            ) {
                Text("Nouvelle ligne")
            }
            Button(
                onClick = { showNextMonthConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Nouveau mois")
            }
        }

        if (showNextMonthConfirm) {
            AlertDialog(
                onDismissRequest = { showNextMonthConfirm = false },
                containerColor = Color(0xFF1A1D24),
                title = { Text("Passer au mois suivant ?", color = Color.White) },
                text = { Text("Le mois en cours sera archivé et les lignes réinitialisées. Cette action est irréversible.", color = Color.Gray) },
                confirmButton = {
                    Button(
                        onClick = {
                            if (activeMode == "solo") viewModel.resetSoloMonth()
                            else viewModel.passToNextMonth()
                            showNextMonthConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) { Text("Confirmer", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showNextMonthConfirm = false }) {
                        Text("Annuler", color = Color.Gray)
                    }
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // --- Onglets ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { (type, label) ->
                val isSelected = selectedTab == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isSelected && type == "current" -> if (activeMode == "solo") Color(0xFF5E35B1) else Color(0xFF1565C0)
                                isSelected && type == "saving" -> Color(0xFF2E7D32)
                                else -> Color.Transparent
                            }
                        )
                        .clickable { selectedTab = type }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- Liste scrollable ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredLines, key = { it.id }) { line ->
                SwipeToDeleteContainer(
                    remainingAmount = line.remainingAmount,
                    onDeleteConfirmed = { viewModel.deleteBudgetLine(line.id) }
                ) {
                    if (activeMode == "solo" && selectedTab == "current") {
                        // Tuile avec case à cocher pour les récurrentes solo
                        SoloRecurringTile(
                            line = line,
                            onTogglePaid = { viewModel.toggleLinePaid(line.id, !line.paidThisMonth) },
                            onEdit = { lineToEdit = it; showAddSheet = true }
                        )
                    } else {
                        BudgetLineTile(
                            line = line,
                            viewModel = viewModel,
                            onEdit = { lineToEdit = it; showAddSheet = true }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddBudgetLineSheet(
            onDismiss = { showAddSheet = false },
            onValidate = { title, periodicity, cost, baseAmount, payer, type ->
                if (lineToEdit == null) {
                    viewModel.addBudgetLine(title, periodicity, cost, baseAmount, payer, type)
                } else {
                    viewModel.updateBudgetLine(lineToEdit!!.id, title, periodicity, cost, baseAmount, payer, type)
                }
                showAddSheet = false
                lineToEdit = null
            },
            viewModel = viewModel,
            lineToEdit = lineToEdit,
            defaultType = selectedTab
        )
    }
}

// ── Tuile récurrente solo avec case à cocher ──────────────
@Composable
fun SoloRecurringTile(
    line: BudgetLine,
    onTogglePaid: () -> Unit,
    onEdit: (BudgetLine) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (line.paidThisMonth) Color(0xFF0F2F0F) else Color(0xFF1E1E1E),
                RoundedCornerShape(12.dp)
            )
            .clickable { onEdit(line) }  // ← toute la ligne est cliquable
            .padding(horizontal = 16.dp, vertical = 12.dp),  // ← padding réduit
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Case à cocher
        Checkbox(
            checked = line.paidThisMonth,
            onCheckedChange = { onTogglePaid() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF4CAF50),
                uncheckedColor = Color.Gray
            )
        )

        // Titre
        Text(
            line.title,
            color = if (line.paidThisMonth) Color.Gray else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = if (line.paidThisMonth) {
                androidx.compose.ui.text.style.TextDecoration.LineThrough
            } else null,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        )

        // Coût mensuel à droite
        Text(
            "${String.format("%.2f", line.monthlyCost)} €",
            color = if (line.paidThisMonth) Color.Gray else Color(0xFF4CAF50),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}