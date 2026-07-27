package com.example.familybudget20.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ---- PALETTE ----
val BluePrimary = Color(0xFF2196F3)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkText = Color.White

// ---- COLOR SCHEME ----
private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    secondary = BluePrimary,
    onSecondary = Color.White
)

// ---- TEXTFIELD COLORS ----
@Composable
fun darkTextFieldColors() = TextFieldDefaults.colors(
    focusedIndicatorColor = BluePrimary,
    unfocusedIndicatorColor = Color.Gray,
    focusedLabelColor = BluePrimary,
    cursorColor = BluePrimary,
    focusedTextColor = DarkText,
    unfocusedTextColor = DarkText,
    focusedContainerColor = DarkSurface,
    unfocusedContainerColor = DarkSurface
)

// ---- BUTTON COLORS ----
@Composable
fun darkButtonColors() = ButtonDefaults.buttonColors(
    containerColor = BluePrimary,
    contentColor = Color.White
)

// ---- THEME WRAPPER ----
@Composable
fun FamilyBudget20Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
