package com.example.familybudget20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.familybudget20.model.BudgetLine
import com.example.familybudget20.model.Transaction
import com.example.familybudget20.viewmodel.StartupViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: StartupViewModel,
    modifier: Modifier = Modifier
) {
    val budgetLines = viewModel.budgetLines.collectAsState().value
    val transactions = viewModel.transactions.collectAsState().value
    val profile = viewModel.userProfile.collectAsState().value
    val partnerProfile = viewModel.partnerProfile.collectAsState().value

    var selectedLine by remember { mutableStateOf<BudgetLine?>(null) }
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ajout") }
    var selectedAuthor by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val creatorName = if (profile?.isCreator == true) profile.name else partnerProfile?.name ?: "Créateur"
    val partnerName = if (profile?.isCreator == true) partnerProfile?.name ?: "Partenaire" else profile?.name ?: "Partenaire"
    var annotation by remember { mutableStateOf("") }

    LaunchedEffect(creatorName) {
        if (selectedAuthor.isEmpty()) selectedAuthor = creatorName
    }

    if (showHistory) {
        TransactionHistorySheet(
            transactions = transactions,
            onDismiss = { showHistory = false }
        )
    }

    Column(
        modifier = modifier
            .background(Color(0xFF121212))
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        // ── Header ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Dépenses",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showHistory = true }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Historique",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // ── Liste lignes avec cadre ───────────────────────────
        Text("Ligne budgétaire", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        val listState = rememberLazyListState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(budgetLines) { _, line ->
                    val isSelected = selectedLine?.id == line.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF1A2E3A) else Color(0xFF1E1E1E))
                            .clickable {
                                selectedLine = line
                                errorMessage = ""
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(line.title, color = Color.White, fontSize = 15.sp)
                            Text(
                                "Solde : ${String.format("%.2f", line.remainingAmount)} €",
                                color = if (line.remainingAmount >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                                fontSize = 13.sp
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF2196F3))
                            )
                        }
                    }
                }
            }

            // Fondu haut
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF121212), Color.Transparent)
                        )
                    )
            )

            // Fondu bas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF121212))
                        )
                    )
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Type : Ajout / Dépense ────────────────────────────
        Text("Type", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("ajout" to "💰 Ajout", "depense" to "💸 Dépense").forEach { (value, label) ->
                val isSelected = selectedType == value
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) {
                                if (value == "ajout") Color(0xFF1B5E20) else Color(0xFF7F0000)
                            } else Color(0xFF1E1E1E)
                        )
                        .clickable { selectedType = value }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        label,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Montant ───────────────────────────────────────────
        Text("Montant (€)", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            value = amount,
            onValueChange = { amount = it; errorMessage = "" },
            label = { Text("Ex: 150") },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color(0xFF2196F3),
                unfocusedIndicatorColor = Color.Gray,
                focusedLabelColor = Color(0xFF2196F3),
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // ── Qui fait cette action ? ───────────────────────────
        Text("Qui fait cette action ?", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                creatorName to (profile?.let {
                    if (it.isCreator) it.color else partnerProfile?.color
                } ?: "#2196F3"),
                partnerName to (profile?.let {
                    if (!it.isCreator) it.color else partnerProfile?.color
                } ?: "#2196F3")
            ).forEach { (name, colorHex) ->
                val isSelected = selectedAuthor == name
                val profileColor = try {
                    Color(android.graphics.Color.parseColor(colorHex ?: "#2196F3"))
                } catch (e: Exception) {
                    Color(0xFF2196F3)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) profileColor else Color(0xFF1E1E1E))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.Transparent else profileColor,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedAuthor = name }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

// ── Annotation ────────────────────────────────────────
        Text("Annotation (optionnel)", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = annotation,
            onValueChange = { annotation = it },
            label = { Text("Ex: Intermarché, remboursement...") },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color(0xFF2196F3),
                unfocusedIndicatorColor = Color.Gray,
                focusedLabelColor = Color(0xFF2196F3),
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Bouton valider ────────────────────────────────────
        Button(
            onClick = {
                val parsedAmount = amount.replace(",", ".").toDoubleOrNull()
                when {
                    selectedLine == null -> errorMessage = "Sélectionne une ligne"
                    amount.isBlank() -> errorMessage = "Indique un montant"
                    parsedAmount == null || parsedAmount <= 0 -> errorMessage = "Montant invalide"
                    else -> {
                        viewModel.addTransaction(
                            line = selectedLine!!,
                            amount = parsedAmount,
                            type = selectedType,
                            authorName = selectedAuthor,
                            annotation = annotation
                        )
                        amount = ""
                        annotation = ""
                        selectedLine = null
                        errorMessage = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Text("Valider", color = Color.White, fontSize = 16.sp)
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── BottomSheet historique transactions ───────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistorySheet(
    transactions: List<Transaction>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "100 derniers mouvements",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (transactions.isEmpty()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Aucun mouvement pour le moment", color = Color.Gray)
                }
            }

            transactions.forEach { transaction ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            transaction.lineTitle,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Par ${transaction.authorName} · ${formatDate(transaction.createdAt)}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        if (transaction.annotation.isNotEmpty()) {
                            Text(
                                "📝 ${transaction.annotation}",
                                color = Color(0xFF90A4AE),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Text(
                        text = if (transaction.type == "ajout") "+${String.format("%.2f", transaction.amount)} €"
                        else "-${String.format("%.2f", transaction.amount)} €",
                        color = if (transaction.type == "ajout") Color(0xFF4CAF50) else Color(0xFFE53935),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

fun formatDate(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}