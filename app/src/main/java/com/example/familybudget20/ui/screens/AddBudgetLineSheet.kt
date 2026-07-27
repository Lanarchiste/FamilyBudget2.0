package com.example.familybudget20.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.familybudget20.model.BudgetLine
import com.example.familybudget20.viewmodel.StartupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetLineSheet(
    onDismiss: () -> Unit,
    onValidate: (String, String, Double, Double, String, String) -> Unit,  // ← +type
    viewModel: StartupViewModel,
    lineToEdit: BudgetLine? = null,
    defaultType: String = "current"  // ← nouveau
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(lineToEdit?.title ?: "") }
    var cost by remember { mutableStateOf(lineToEdit?.monthlyCost?.toString() ?: "") }
    var baseAmount by remember { mutableStateOf(lineToEdit?.baseAmount?.toString() ?: "") }
    var periodicity by remember { mutableStateOf(lineToEdit?.periodicity ?: "monthly") }
    var payer by remember { mutableStateOf(lineToEdit?.payer ?: "both") }
    var type by remember { mutableStateOf(lineToEdit?.type ?: defaultType) }  // ← nouveau

    val profile = viewModel.userProfile.collectAsState().value
    val partnerProfile = viewModel.partnerProfile.collectAsState().value
    val activeMode = viewModel.activeMode.collectAsState().value

    val creatorName = if (profile?.isCreator == true) profile.name
    else partnerProfile?.name ?: "Créateur"
    val partnerName = if (profile?.isCreator == true) partnerProfile?.name ?: "Partenaire"
    else profile?.name ?: "Partenaire"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = if (lineToEdit == null) "Nouvelle ligne" else "Modifier la ligne",
                color = Color.White,
                fontSize = 22.sp
            )

            // --- Type de ligne ---
            Text("Type de ligne", color = Color.Gray, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "current" to "💙 Courant",
                    "saving" to "💚 Épargne"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = type == value,
                        onClick = { type = value },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (value) {
                                "current" -> Color(0xFF1565C0)
                                "saving" -> Color(0xFF2E7D32)
                                else -> Color(0xFF2196F3)
                            },
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // --- Titre ---
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // --- Coût ---
            OutlinedTextField(
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                value = cost,
                onValueChange = { cost = it },
                label = { Text("Coût (€)") },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // --- Montant déjà présent ---
            OutlinedTextField(
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                value = baseAmount,
                onValueChange = { baseAmount = it },
                label = { Text("Montant déjà présent (€)") },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // --- Périodicité ---
            Text("Périodicité", color = Color.Gray, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "monthly" to "Mensuel",
                    "quarterly" to "Trimestriel",
                    "yearly" to "Annuel"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = periodicity == value,
                        onClick = { periodicity = value },
                        label = { Text(label) }
                    )
                }
            }

            // --- Qui paye (uniquement en mode famille et type courant) ---
            if (activeMode == "family" && type == "current") {
                Text("Qui paye ?", color = Color.Gray, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "creator" to creatorName,
                        "partner" to partnerName,
                        "both" to "Les deux"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = payer == value,
                            onClick = { payer = value },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // --- Bouton ---
            Button(
                onClick = {
                    onValidate(
                        title,
                        periodicity,
                        cost.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        baseAmount.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        payer,
                        type   // ← nouveau
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text(
                    text = if (lineToEdit == null) "Ajouter" else "Modifier",
                    color = Color.White
                )
            }
        }
    }
}