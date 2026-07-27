package com.example.familybudget20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.familybudget20.viewmodel.StartupViewModel

@Composable
fun HistoryScreen(
    viewModel: StartupViewModel,
    modifier: Modifier = Modifier
) {

    val historyMonths = viewModel.historyMonths.collectAsState().value
    val historyLines = viewModel.historyLines.collectAsState().value
    var selectedMonth by remember { mutableStateOf<String?>(null) }
    val lazyListState = rememberLazyListState()

    // Sélectionne automatiquement le mois le plus récent au chargement
    LaunchedEffect(historyMonths) {
        viewModel.loadHistoryMonths()
        if (selectedMonth == null && historyMonths.isNotEmpty()) {
            selectedMonth = historyMonths.first()
            viewModel.loadHistoryLines(historyMonths.first())
        }
    }

    Column(
        modifier = modifier  // ← remplace Modifier.fillMaxSize() par modifier
            .background(Color(0xFF121212))
            .padding(top = 16.dp)
    ) {

        // ── Titre ─────────────────────────────────────────────
        Text(
            text = "Historique",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        // ── Onglets mois (scroll horizontal) ──────────────────
        if (historyMonths.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucun historique pour le moment", color = Color.Gray)
            }
        } else {
            LazyRow(
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(historyMonths) { month ->
                    val isSelected = selectedMonth == month
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color(0xFF2196F3) else Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedMonth = month
                                viewModel.loadHistoryLines(month)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatMonth(month),
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Contenu du mois sélectionné ────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            if (selectedMonth == null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Sélectionne un mois", color = Color.Gray)
                }
            } else if (historyLines.isEmpty()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Chargement...", color = Color.Gray)
                }
            } else {

                // Année affichée au dessus si on change d'année
                val year = selectedMonth!!.split("-")[0]
                Text(
                    text = year,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                historyLines.forEach { line ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(line.title, color = Color.White, fontSize = 16.sp)
                            Text(
                                "Mensuel : ${String.format("%.2f", line.monthlyCost)} €",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                            Text(
                                "Payé par : ${viewModel.getPayerName(line.payer)}",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "${String.format("%.2f", line.remainingAmount)} €",
                            color = if (line.remainingAmount >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ── Total du mois ──────────────────────────────
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A2733), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Total épargné",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    val total = historyLines.sumOf { it.remainingAmount }
                    Text(
                        "${String.format("%.2f", total)} €",
                        color = if (total >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

fun formatMonth(month: String): String {
    val parts = month.split("-")
    if (parts.size != 2) return month
    val months = listOf(
        "Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
        "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc"
    )
    val monthIndex = parts[1].toIntOrNull()?.minus(1) ?: return month
    return months.getOrNull(monthIndex) ?: month
}