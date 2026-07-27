package com.example.familybudget20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.familybudget20.ui.navigation.AppNavHost
import com.example.familybudget20.ui.theme.FamilyBudget20Theme
import com.example.familybudget20.viewmodel.StartupViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val auth = FirebaseAuth.getInstance()

        // Si déjà connecté (session existante), on lance directement
        if (auth.currentUser != null) {
            launchApp()
            return
        }

        // Sinon on attend la connexion anonyme
        auth.signInAnonymously()
            .addOnSuccessListener { launchApp() }
            .addOnFailureListener {
                // En cas d'échec réseau, on réessaie au prochain lancement
                launchApp()
            }
    }

    private fun launchApp() {
        setContent {
            FamilyBudget20Theme {
                val startupViewModel: StartupViewModel = viewModel()
                AppNavHost(startupViewModel = startupViewModel)
            }
        }
    }
}

// POPUP de MaJ



