package com.example.familybudget20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.familybudget20.ui.theme.ColorPicker
import com.example.familybudget20.viewmodel.StartupViewModel

@Composable
fun SoloOnboardingScreen(viewModel: StartupViewModel) {

    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#7C4DFF") }  // ← violet par défaut
    var salary by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("") }
    var errorName by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Création du profil",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Mode Solo",
            color = Color(0xFF7C4DFF),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(32.dp))

        // ── Pseudo ────────────────────────────────────────────
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; errorName = false },
            label = { Text("Pseudo") },
            singleLine = true,
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        if (errorName) {
            Text("Le pseudo est obligatoire", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))

        // ── Salaire ───────────────────────────────────────────
        OutlinedTextField(
            value = salary,
            onValueChange = { salary = it },
            label = { Text("Salaire mensuel (€)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // ── Solde compte courant ──────────────────────────────
        OutlinedTextField(
            value = initialBalance,
            onValueChange = { initialBalance = it },
            label = { Text("Solde actuel du compte courant (€)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        Text(
            "Tu pourras le modifier à tout moment",
            color = Color.Gray,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(16.dp))

        // ── Couleur ───────────────────────────────────────────
        Text("Choisis ta couleur", color = Color.White)
        ColorPicker(selected = color, onSelect = { color = it })

        Spacer(Modifier.height(32.dp))

        // ── Bouton ────────────────────────────────────────────
        Button(
            onClick = {
                if (name.isBlank()) {
                    errorName = true
                    return@Button
                }
                viewModel.finishSoloOnboarding(
                    name = name,
                    color = color,
                    salary = salary.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    initialBalance = initialBalance.replace(",", ".").toDoubleOrNull() ?: 0.0
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
        ) {
            Text("Commencer en Solo", color = Color.White, fontSize = 16.sp)
        }
    }
}