package com.example.familybudget20.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Star

@Composable
fun BottomNavBar(
    selected: String,
    onSelect: (String) -> Unit,
    activeMode: String = "family"  // ← nouveau paramètre
) {
    NavigationBar(containerColor = Color(0xFF1A1A1A)) {
        NavigationBarItem(
            selected = selected == "home",
            onClick = { onSelect("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Accueil", fontSize = 14.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            selected = selected == "budget",
            onClick = { onSelect("budget") },
            icon = { Icon(Icons.Filled.List, contentDescription = null) },
            label = { Text("Budget", fontSize = 14.sp) }
        )

        NavigationBarItem(
            selected = selected == "history",
            onClick = { onSelect("history") },
            icon = { Icon(Icons.Filled.List, contentDescription = null) },
            label = { Text("Historique", fontSize = 14.sp) }
        )

        // ← Change selon le mode
        NavigationBarItem(
            selected = selected == "expenses",
            onClick = { onSelect("expenses") },
            icon = {
                Icon(
                    imageVector = if (activeMode == "solo") Icons.Default.Star else Icons.Default.ShoppingCart,
                    contentDescription = null
                )
            },
            label = {
                Text(
                    if (activeMode == "solo") "Compte" else "Dépenses",
                    fontSize = 14.sp
                )
            }
        )
    }
}
