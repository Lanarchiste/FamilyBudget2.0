package com.example.familybudget20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
fun SoloHistoryScreen(
    viewModel: StartupViewModel,
    modifier: Modifier = Modifier
) {
    val historyMonths = viewModel.soloHistoryMonths.collectAsState().value
    val historyBills = viewModel.soloHistoryBills.collectAsState().value
    val historySavings = viewModel.soloHistorySavings.collectAsState().value
    val historyExpensesTotal = viewModel.soloHistoryExpensesTotal.collectAsState().value
    var selectedMonth by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadSoloHistoryMonths()
    }

    LaunchedEffect(historyMonths) {
        if (selectedMonth == null && historyMonths.isNotEmpty()) {
            selectedMonth = historyMonths.first()
            viewModel.loadSoloHistoryDetails(historyMonths.first())
        }
    }

    Column(
        modifier = modifier
            .background(Color(0xFF121212))
            .padding(top = 16.dp)
    ) {

        Text(
            text = "Historique",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        if (historyMonths.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucun historique pour le moment", color = Color.Gray)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(historyMonths) { month ->
                    val isSelected = selectedMonth == month
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color(0xFF7C4DFF) else Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedMonth = month
                                viewModel.loadSoloHistoryDetails(month)
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

        if (selectedMonth != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                val year = selectedMonth!!.split("-")[0]
                Text(text = year, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))

                // ── Factures payées ─────────────────────────────
                Text("Factures payées", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (historyBills.isEmpty()) {
                    Text("Aucune facture payée ce mois-là", color = Color.Gray, fontSize = 13.sp)
                } else {
                    historyBills.forEach { (title, amount) ->
                        HistoryTile(title = title, amount = amount, positive = false)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Épargne (gain du mois) ──────────────────────
                Text("Épargne", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (historySavings.isEmpty()) {
                    Text("Aucune épargne validée ce mois-là", color = Color.Gray, fontSize = 13.sp)
                } else {
                    historySavings.forEach { (title, amount) ->
                        HistoryTile(title = title, amount = amount, positive = true)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Dépenses ponctuelles (résumé) ───────────────
                Text("Dépenses", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                HistoryTile(title = "Dépenses du mois", amount = historyExpensesTotal, positive = false)

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HistoryTile(title: String, amount: Double, positive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 15.sp)
        Text(
            text = "${if (positive) "+" else "-"}${String.format("%.2f", amount)} €",
            color = if (positive) Color(0xFF4CAF50) else Color(0xFFE53935),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
