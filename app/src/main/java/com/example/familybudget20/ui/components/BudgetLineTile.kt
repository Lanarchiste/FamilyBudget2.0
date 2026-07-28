package com.example.familybudget20.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.familybudget20.model.BudgetLine
import com.example.familybudget20.viewmodel.StartupViewModel

@Composable
fun BudgetLineTile(
    line: BudgetLine,
    viewModel: StartupViewModel,
    onEdit: (BudgetLine) -> Unit
) {
    val activeMode = viewModel.activeMode.collectAsState().value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
            .clickable { onEdit(line) },
        verticalAlignment = Alignment.Top
    ) {

        Column(modifier = Modifier.weight(1f)) {

            // Ligne du haut : Titre + Restant aligné à droite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = line.title,
                    color = Color.White,
                    fontSize = 20.sp
                )

                Text(
                    text = "${String.format("%.2f", line.remainingAmount)} €",
                    color = if (line.remainingAmount >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            // Ligne du bas : coût mensuel + payer
            Text(
                text = "Mensuel : ${String.format("%.2f", line.monthlyCost)} €",
                color = Color.Gray,
                fontSize = 14.sp
            )

            // Le champ "payer" n'existe pas côté solo (toujours "both" par défaut,
            // non éditable) : pas la peine de l'afficher.
            if (activeMode != "solo") {
                Text(
                    text = "Payé par : ${viewModel.getPayerName(line.payer)}",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis  // ← affiche "Payé par : Tipha..." proprement
                )
            }
        }

        // Icône d’édition
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Modifier",
            tint = Color(0xFF2196F3),
            modifier = Modifier.size(22.dp)
        )
    }
}


