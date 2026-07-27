package com.example.familybudget20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.familybudget20.viewmodel.StartupViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AccountScreen(
    viewModel: StartupViewModel,
    modifier: Modifier = Modifier
) {
    val transactions = viewModel.transactions.collectAsState().value
    val soloAccountBalance = viewModel.soloAccountBalance.collectAsState().value

    var showWarning by remember { mutableStateOf(false) }
    var showBalanceEdit by remember { mutableStateOf(false) }
    var newBalance by remember { mutableStateOf("") }

    // ── Popup avertissement ───────────────────────────────────
    if (showWarning) {
        AlertDialog(
            onDismissRequest = { showWarning = false },
            containerColor = Color(0xFF1A1D24),
            title = { Text("⚠️ Attention", color = Color.White) },
            text = {
                Text(
                    "Modifier le solde manuellement écrasera l'historique des calculs. Le solde affiché ne correspondra plus aux mouvements enregistrés. Veux-tu continuer ?",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = { showWarning = false; showBalanceEdit = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Continuer quand même", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showWarning = false }) {
                    Text("Annuler", color = Color.Gray)
                }
            }
        )
    }

    // ── Popup saisie nouveau solde ────────────────────────────
    if (showBalanceEdit) {
        AlertDialog(
            onDismissRequest = { showBalanceEdit = false },
            containerColor = Color(0xFF1A1D24),
            title = { Text("Modifier le solde", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newBalance,
                    onValueChange = { newBalance = it },
                    label = { Text("Nouveau solde (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color(0xFF7C4DFF),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedLabelColor = Color(0xFF7C4DFF),
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val balance = newBalance.replace(",", ".").toDoubleOrNull()
                        if (balance != null) {
                            viewModel.setSoloAccountBalance(balance)
                            showBalanceEdit = false
                            newBalance = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                ) { Text("Confirmer", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showBalanceEdit = false }) {
                    Text("Annuler", color = Color.Gray)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            "Compte courant",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // ── Tuile solde (cliquable) ───────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1D24))
                .clickable { showWarning = true }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Solde disponible",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${String.format("%.2f", soloAccountBalance)} €",
                    color = if (soloAccountBalance >= 0) Color.White else Color(0xFFE53935),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Light
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Appuie pour modifier",
                    color = Color.Gray.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Historique des mouvements",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(8.dp))

        // ── Historique ────────────────────────────────────────
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucun mouvement pour le moment", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions) { transaction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1E1E))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                transaction.annotation.ifEmpty { "💳" },
                                fontSize = 24.sp
                            )
                            Column {
                                Text(
                                    transaction.lineTitle,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    formatTransactionDate(transaction.createdAt),
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            text = if (transaction.type == "ajout")
                                "+${String.format("%.2f", transaction.amount)} €"
                            else
                                "-${String.format("%.2f", transaction.amount)} €",
                            color = if (transaction.type == "ajout") Color(0xFF4CAF50) else Color(0xFFE53935),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun formatTransactionDate(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}