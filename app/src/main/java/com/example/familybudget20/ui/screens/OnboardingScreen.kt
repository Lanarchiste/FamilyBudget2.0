package com.example.familybudget20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialException
import com.example.familybudget20.ui.theme.ColorPicker
import com.example.familybudget20.viewmodel.StartupViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(viewModel: StartupViewModel, onCancel: (() -> Unit)? = null) {

    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#2196F3") }
    var salary by remember { mutableStateOf("") }
    var familyCode by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    var errorName by remember { mutableStateOf(false) }
    var errorCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLinkingGoogle by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("Création du profil", color = Color.White, fontSize = 26.sp)

        Spacer(Modifier.height(24.dp))

        // ── Connexion Google (optionnelle) ────────────────────
        Button(
            onClick = {
                googleError = null
                isLinkingGoogle = true
                scope.launch {
                    try {
                        val identity = requestGoogleIdentity(context)
                        name = identity.givenName ?: identity.displayName ?: name
                        viewModel.linkOrRecoverGoogleAccount(identity.idToken) { success, message ->
                            isLinkingGoogle = false
                            if (!success) googleError = message
                        }
                    } catch (e: GetCredentialException) {
                        isLinkingGoogle = false
                        googleError = e.message ?: "Connexion Google annulée ou impossible"
                    } catch (e: IllegalStateException) {
                        isLinkingGoogle = false
                        googleError = e.message ?: "Connexion Google impossible"
                    }
                }
            },
            enabled = !isLinkingGoogle,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            if (isLinkingGoogle) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("Se connecter avec Google", color = Color.White)
            }
        }
        if (googleError != null) {
            Text(googleError!!, color = Color.Red, fontSize = 12.sp)
        }
        Text(
            "Optionnel — permet de récupérer tes données si tu changes de téléphone. Tu peux aussi continuer sans, et le faire plus tard dans les réglages.",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(20.dp))

        // ── Toggle Créer / Rejoindre ──────────────────────────
        Row(
            modifier = Modifier
                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            ToggleButton(
                label = "Créer une famille",
                selected = !isJoining,
                onClick = {
                    isJoining = false
                    errorCode = ""
                }
            )
            ToggleButton(
                label = "Rejoindre",
                selected = isJoining,
                onClick = {
                    isJoining = true
                    errorCode = ""
                }
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Champ Pseudo ──────────────────────────────────────
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

        // ── Champ Salaire ─────────────────────────────────────
        OutlinedTextField(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            value = salary,
            onValueChange = { salary = it },
            label = { Text("Salaire (€)") },
            singleLine = true,
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // ── Couleur ───────────────────────────────────────────
        Text("Choisis ta couleur", color = Color.White)
        ColorPicker(selected = color, onSelect = { color = it })

        Spacer(Modifier.height(16.dp))

        // ── Champ code famille (seulement si "Rejoindre") ─────
        if (isJoining) {
            OutlinedTextField(
                value = familyCode,
                onValueChange = { familyCode = it.uppercase(); errorCode = "" },
                label = { Text("Code famille (ex: AB12CD)") },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }

        if (errorCode.isNotEmpty()) {
            Text(errorCode, color = Color.Red, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(32.dp))

        // ── Bouton principal ──────────────────────────────────
        Button(
            onClick = {
                if (name.isBlank()) { errorName = true; return@Button }

                if (isJoining) {
                    if (familyCode.length != 6) {
                        errorCode = "Le code doit faire 6 caractères"
                        return@Button
                    }
                    isLoading = true
                    viewModel.joinFamily(
                        code = familyCode,
                        name = name,
                        color = color,
                        salary = salary.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        onError = { msg ->
                            errorCode = msg
                            isLoading = false
                        }
                    )
                } else {
                    isLoading = true
                    viewModel.finishOnboarding(
                        name = name,
                        color = color,
                        salary = salary.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        onError = { msg ->
                            errorCode = msg
                            isLoading = false
                        }
                    )
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text(if (isJoining) "Rejoindre la famille" else "Créer ma famille", color = Color.White)
            }
        }
    }

        if (onCancel != null) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Annuler", tint = Color.White)
            }
        }
    }
}

// ── Composant bouton toggle ────────────────────────────────
@Composable
fun ToggleButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF2196F3) else Color.Transparent,
            contentColor = if (selected) Color.White else Color.Gray
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

// ── Couleurs communes des champs ──────────────────────────
@Composable
fun fieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedIndicatorColor = Color(0xFF2196F3),
    unfocusedIndicatorColor = Color.Gray,
    focusedLabelColor = Color(0xFF2196F3),
    unfocusedLabelColor = Color.Gray,
    cursorColor = Color.White,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)