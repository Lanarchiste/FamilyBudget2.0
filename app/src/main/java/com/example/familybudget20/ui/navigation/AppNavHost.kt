package com.example.familybudget20.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.familybudget20.ui.screens.*
import com.example.familybudget20.viewmodel.StartupViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun AppNavHost(startupViewModel: StartupViewModel) {

    val state = startupViewModel.startupState.collectAsState().value
    var showSettings by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("home") }
    val activeMode by startupViewModel.activeMode.collectAsState()
    val userProfile by startupViewModel.userProfile.collectAsState()

    // Popup changelog
    val context = LocalContext.current
    var showChangelog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastVersion = prefs.getString("last_version", "")
        val currentVersion = context.packageManager
            .getPackageInfo(context.packageName, 0).versionName
        if (lastVersion != currentVersion) {
            showChangelog = true
            prefs.edit().putString("last_version", currentVersion).apply()
        }
    }

    if (showChangelog) {
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            containerColor = Color(0xFF1A1D24),
            title = { Text("✨ Nouveautés v0.2", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• L'appli peut enfin se lancer en solo uniquement, sans passer par la création d'une famille", color = Color.Gray)
                    Text("• Solo : nouvel historique mensuel (factures payées, gain d'épargne, dépenses du mois)", color = Color.Gray)
                    Text("• Solo : le mois peut suivre tes propres dates (ex: paie le 17) plutôt que le mois calendaire", color = Color.Gray)
                    Text("• Solo : le solde du compte ne reste plus bloqué après une réinitialisation", color = Color.Gray)
                    Text("• Divers correctifs de stabilité et d'affichage en mode solo", color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showChangelog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) { Text("Super !", color = Color.White) }
            }
        )
    }

    when (state) {

        is StartupViewModel.StartupState.Loading -> SplashScreen()

        is StartupViewModel.StartupState.Welcome -> WelcomeScreen(
            onFinished = { startupViewModel.goToOnboarding() },
            onSoloMode = { startupViewModel.goToSoloOnboarding() }  // ← nouveau
        )

        is StartupViewModel.StartupState.Onboarding -> OnboardingScreen(
            viewModel = startupViewModel,
            // On ne propose "Annuler" que si on arrive ici avec un profil déjà
            // existant (ex: utilisateur solo qui a cliqué sur "Famille").
            onCancel = if (userProfile != null) {
                { startupViewModel.cancelOnboarding() }
            } else null
        )
        is StartupViewModel.StartupState.SoloOnboarding -> SoloOnboardingScreen(
            viewModel = startupViewModel
        )

        is StartupViewModel.StartupState.Home -> {

            var showQuickTransaction by remember { mutableStateOf(false) }

            if (showQuickTransaction) {
                QuickTransactionSheet(
                    viewModel = startupViewModel,
                    onDismiss = { showQuickTransaction = false }
                )
            }

            Scaffold(
                bottomBar = {
                    BottomNavBar(
                        selected = selectedTab,
                        onSelect = { selectedTab = it },
                        activeMode = activeMode
                    )
                },
                floatingActionButton = {
                    if (activeMode == "solo" && (selectedTab == "home" || selectedTab == "expenses")) {
                        FloatingActionButton(
                            onClick = { showQuickTransaction = true },
                            containerColor = Color(0xFF7C4DFF),
                            contentColor = Color.White,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Nouveau mouvement",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                floatingActionButtonPosition = FabPosition.Center
            ) { innerPadding ->

                when (selectedTab) {

                    "home" -> HomeScreen(
                        viewModel = startupViewModel,
                        onOpenSettings = { showSettings = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )

                    "budget" -> BudgetScreen(
                        viewModel = startupViewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )

                    "history" -> if (activeMode == "solo") {
                        SoloHistoryScreen(
                            viewModel = startupViewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    } else {
                        HistoryScreen(
                            viewModel = startupViewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }

                    "expenses" -> if (activeMode == "solo") {
                        AccountScreen(
                            viewModel = startupViewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    } else {
                        ExpensesScreen(
                            viewModel = startupViewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }
                }

                if (showSettings) {
                    SettingsScreen(
                        profile = startupViewModel.userProfile.collectAsState().value,
                        isAnonymous = startupViewModel.isAnonymousAccount.collectAsState().value,
                        linkedEmail = startupViewModel.linkedAccountEmail.collectAsState().value,
                        onLinkGoogle = { idToken, onResult ->
                            startupViewModel.linkOrRecoverGoogleAccount(idToken, onResult)
                        },
                        onSave = { name, salary, color ->
                            startupViewModel.saveUser(
                                name = name,
                                color = color,
                                familyId = startupViewModel.userProfile.value?.familyId ?: "XXXXXX",
                                isCreator = startupViewModel.userProfile.value?.isCreator ?: false,
                                salary = salary
                            )
                            showSettings = false
                        },
                        onDismiss = { showSettings = false }
                    )
                }
            }
        }
    }
}