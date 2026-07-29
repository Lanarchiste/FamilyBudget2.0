package com.example.familybudget20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.familybudget20.model.transactionCategories
import com.example.familybudget20.viewmodel.StartupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTransactionSheet(
    viewModel: StartupViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedMode by remember { mutableStateOf("compte") } // "compte" | "epargne"
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("depense") }
    var selectedCategory by remember { mutableStateOf<com.example.familybudget20.model.TransactionCategory?>(null) }
    var selectedDirection by remember { mutableStateOf("verser") } // "verser" | "retirer"
    var selectedSavingLineId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    val accentColor = Color(0xFF7C4DFF)
    val savingLines = viewModel.budgetLines.collectAsState().value.filter { it.type == "saving" }
    val selectedSavingLine = savingLines.firstOrNull { it.id == selectedSavingLineId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                "Nouveau mouvement",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            // ── Mode : Compte courant / Épargne ───────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                listOf("compte" to "💳 Compte courant", "epargne" to "🏦 Épargne").forEach { (value, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedMode == value) accentColor.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { selectedMode = value; errorMessage = "" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (selectedMode == value) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            if (selectedMode == "compte") {
                // ── Type : Dépense / Gain ─────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    listOf("depense" to "💸 Dépense", "ajout" to "💰 Gain").forEach { (value, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        selectedType == value && value == "depense" -> Color(0xFF7F0000)
                                        selectedType == value && value == "ajout" -> Color(0xFF1B5E20)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { selectedType = value }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (selectedType == value) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            } else {
                // ── Direction : Retirer / Verser ──────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    listOf("retirer" to "↙️ Retirer", "verser" to "↗️ Verser").forEach { (value, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        selectedDirection == value && value == "retirer" -> Color(0xFF7F0000)
                                        selectedDirection == value && value == "verser" -> Color(0xFF1B5E20)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { selectedDirection = value }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (selectedDirection == value) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ── Montant ───────────────────────────────────────
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; errorMessage = "" },
                label = { Text("Montant (€)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = accentColor,
                    unfocusedIndicatorColor = Color.Gray,
                    focusedLabelColor = accentColor,
                    unfocusedLabelColor = Color.Gray,
                    cursorColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedMode == "compte") {
                // ── Grille catégories 5x5 ─────────────────────
                Text("Catégorie", color = Color.Gray, fontSize = 13.sp)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactionCategories) { category ->
                        val isSelected = selectedCategory == category
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.3f)
                                    else Color(0xFF1E1E1E)
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) accentColor else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(category.emoji, fontSize = 22.sp)
                            Text(
                                category.label,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            } else {
                // ── Choix de la ligne d'épargne ───────────────
                Text("Ligne d'épargne", color = Color.Gray, fontSize = 13.sp)

                if (savingLines.isEmpty()) {
                    Text(
                        "Aucune ligne d'épargne. Crée-en une dans Budget.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savingLines) { line ->
                            val isSelected = selectedSavingLineId == line.id
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) accentColor.copy(alpha = 0.3f)
                                        else Color(0xFF1E1E1E)
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) accentColor else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedSavingLineId = line.id }
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    line.title,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                                Text(
                                    "${"%.2f".format(line.remainingAmount)} €",
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color(0xFFE53935), fontSize = 13.sp)
            }

            // ── Bouton valider ────────────────────────────────
            Button(
                onClick = {
                    if (selectedMode == "compte") {
                        when {
                            amount.isBlank() -> errorMessage = "Indique un montant"
                            amount.replace(",", ".").toDoubleOrNull() == null -> errorMessage = "Montant invalide"
                            selectedCategory == null -> errorMessage = "Choisis une catégorie"
                            else -> {
                                viewModel.addSoloTransaction(
                                    amount = amount.replace(",", ".").toDouble(),
                                    type = selectedType,
                                    category = selectedCategory!!
                                )
                                onDismiss()
                            }
                        }
                    } else {
                        val parsedAmount = amount.replace(",", ".").toDoubleOrNull()
                        when {
                            amount.isBlank() -> errorMessage = "Indique un montant"
                            parsedAmount == null -> errorMessage = "Montant invalide"
                            selectedSavingLine == null -> errorMessage = "Choisis une ligne d'épargne"
                            selectedDirection == "retirer" && parsedAmount > selectedSavingLine.remainingAmount ->
                                errorMessage = "Solde insuffisant (${"%.2f".format(selectedSavingLine.remainingAmount)} € disponibles)"
                            else -> {
                                viewModel.transferSoloSaving(
                                    lineId = selectedSavingLine.id,
                                    amount = parsedAmount,
                                    direction = selectedDirection
                                )
                                onDismiss()
                            }
                        }
                    }
                },
                enabled = selectedMode != "epargne" || savingLines.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Valider", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}