package com.example.familybudget20.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialException
import com.example.familybudget20.model.UserProfile
import com.example.familybudget20.ui.theme.ColorPicker
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: UserProfile?,
    isAnonymous: Boolean,
    linkedEmail: String?,
    onLinkGoogle: (idToken: String, onResult: (success: Boolean, message: String) -> Unit) -> Unit,
    onSave: (String, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var salary by remember { mutableStateOf(profile?.salary?.toString() ?: "") }
    var color by remember { mutableStateOf(profile?.color ?: "#2196F3") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var googleLinkError by remember { mutableStateOf<String?>(null) }
    var isLinkingGoogle by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // ← ouvre directement en grand
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {

            // --- CONTENU SCROLLABLE ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Paramètres", color = Color.White, fontSize = 22.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color(0xFF2196F3),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedLabelColor = Color(0xFF2196F3),
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.White
                    )
                )

                OutlinedTextField(
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    value = salary,
                    onValueChange = { salary = it },
                    label = { Text("Salaire (€)") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color(0xFF2196F3),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedLabelColor = Color(0xFF2196F3),
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.White
                    )
                )

                Text("Couleur", color = Color.White)

                ColorPicker(
                    selected = color,
                    onSelect = { color = it }
                )

                Text(
                    "Code famille : ${profile?.familyId ?: "—"}",
                    color = Color.Gray
                )

                // --- LIAISON COMPTE GOOGLE ---
                if (isAnonymous) {
                    Button(
                        onClick = {
                            googleLinkError = null
                            isLinkingGoogle = true
                            scope.launch {
                                try {
                                    val identity = requestGoogleIdentity(context)
                                    onLinkGoogle(identity.idToken) { success, message ->
                                        isLinkingGoogle = false
                                        if (!success) googleLinkError = message
                                    }
                                } catch (e: GetCredentialException) {
                                    isLinkingGoogle = false
                                    googleLinkError = e.message ?: "Connexion Google annulée ou impossible"
                                } catch (e: IllegalStateException) {
                                    isLinkingGoogle = false
                                    googleLinkError = e.message ?: "Connexion Google impossible"
                                }
                            }
                        },
                        enabled = !isLinkingGoogle,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text(
                            if (isLinkingGoogle) "Connexion..." else "Lier mon compte Google",
                            color = Color.White
                        )
                    }
                    if (googleLinkError != null) {
                        Text(googleLinkError!!, color = Color.Red, fontSize = 12.sp)
                    }
                } else {
                    Text(
                        "Connecté avec Google : ${linkedEmail ?: "—"}",
                        color = Color.Gray
                    )
                }
            }

            // --- BOUTON FIXE EN BAS ---
            Button(
                onClick = { onSave(name, salary.replace(",", ".").toDoubleOrNull() ?: 0.0, color) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("Enregistrer", color = Color.White)
            }
        }
    }
}



