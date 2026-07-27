package com.example.familybudget20.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.familybudget20.model.BudgetLine
import com.example.familybudget20.ui.components.PaymentTile
import com.example.familybudget20.viewmodel.StartupViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: StartupViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("shortcuts", android.content.Context.MODE_PRIVATE) }

    val profile = viewModel.userProfile.collectAsState().value
    val budgetLines = viewModel.budgetLines.collectAsState().value
    val paymentStatus = viewModel.paymentStatus.collectAsState().value
    val partnerProfile = viewModel.partnerProfile.collectAsState().value
    val balanceSnapshots = viewModel.balanceSnapshots.collectAsState().value
    val activeMode = viewModel.activeMode.collectAsState().value

    var pendingPayment by remember { mutableStateOf<Boolean?>(null) }
    var homeClickCount by remember { mutableStateOf(0) }
    var showDevMenu by remember { mutableStateOf(false) }
    var showFamilyWarning by remember { mutableStateOf(false) }
    var shortcuts by remember { mutableStateOf<List<BudgetLine?>>(listOf(null, null, null)) }
    var showShortcutPicker by remember { mutableStateOf<Int?>(null) }

    val modelProducer = remember { CartesianChartModelProducer() }

    val creatorName = if (profile?.isCreator == true) profile.name else partnerProfile?.name ?: "Créateur"
    val partnerName = if (profile?.isCreator == true) partnerProfile?.name ?: "Partenaire" else profile?.name ?: "Partenaire"
    val hasFamilyId = profile?.familyId?.isNotEmpty() == true
    val soloAccountBalance = viewModel.soloAccountBalance.collectAsState().value

    val currentTotal = budgetLines.filter { it.type == "current" }.sumOf { it.remainingAmount }
    val savingTotal = budgetLines.filter { it.type == "saving" }.sumOf { it.remainingAmount }
    val totalAll = currentTotal + savingTotal

    LaunchedEffect(balanceSnapshots) {
        if (balanceSnapshots.size >= 2) {
            modelProducer.runTransaction {
                lineSeries { series(balanceSnapshots.map { it.second }) }
            }
        }
    }

    LaunchedEffect(budgetLines) {
        shortcuts = (0..2).map { i ->
            val savedId = prefs.getString("shortcut_$i", null)
            budgetLines.firstOrNull { it.id == savedId }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────
    pendingPayment?.let { isCreator ->
        AlertDialog(
            onDismissRequest = { pendingPayment = null },
            containerColor = Color(0xFF1A1D24),
            title = { Text("Confirmer le virement", color = Color.White) },
            text = { Text("As-tu bien effectué ton virement avant de valider ?", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = { viewModel.setPaymentStatus(isCreator, true); pendingPayment = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) { Text("Oui, c'est fait !", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { pendingPayment = null }) { Text("Pas encore", color = Color.Gray) }
            }
        )
    }

    if (showFamilyWarning) {
        AlertDialog(
            onDismissRequest = { showFamilyWarning = false },
            containerColor = Color(0xFF1A1D24),
            title = { Text("Rejoindre une famille ?", color = Color.White) },
            text = { Text("Tu es en mode solo. Veux-tu créer ou rejoindre une famille ?", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = { showFamilyWarning = false; viewModel.goToOnboarding() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) { Text("Oui, continuer", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showFamilyWarning = false }) { Text("Annuler", color = Color.Gray) }
            }
        )
    }

    showShortcutPicker?.let { slotIndex ->
        ShortcutPickerDialog(
            budgetLines = budgetLines,
            currentShortcut = shortcuts[slotIndex],
            onSelect = { line ->
                shortcuts = shortcuts.toMutableList().also { it[slotIndex] = line }
                prefs.edit().putString("shortcut_$slotIndex", line.id).apply()
                showShortcutPicker = null
            },
            onClear = {
                shortcuts = shortcuts.toMutableList().also { it[slotIndex] = null }
                prefs.edit().remove("shortcut_$slotIndex").apply()
                showShortcutPicker = null
            },
            onDismiss = { showShortcutPicker = null }
        )
    }

    if (showDevMenu) {
        DevMenuDialog(viewModel = viewModel, onDismiss = { showDevMenu = false })
    }

    // ── Shimmer ───────────────────────────────────────────────
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmerOffset"
    )

    // ── UI principale ─────────────────────────────────────────
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Header fixe ───────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (activeMode == "family") Color(0xFF2196F3) else Color.Transparent)
                        .clickable {
                            if (activeMode != "family") {
                                if (!hasFamilyId) showFamilyWarning = true
                                else viewModel.switchMode("family")
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("👨‍👩‍👧 Famille", color = Color.White, fontSize = 12.sp,
                        fontWeight = if (activeMode == "family") FontWeight.Bold else FontWeight.Normal)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (activeMode == "solo") Color(0xFF7C4DFF) else Color.Transparent)
                        .clickable { if (activeMode != "solo") viewModel.switchMode("solo") }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("👤 Solo", color = Color.White, fontSize = 12.sp,
                        fontWeight = if (activeMode == "solo") FontWeight.Bold else FontWeight.Normal)
                }
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Paramètres", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // -- le bouton (+) -------------

        var showQuickTransaction by remember { mutableStateOf(false) }

        if (showQuickTransaction) {
            QuickTransactionSheet(
                viewModel = viewModel,
                onDismiss = { showQuickTransaction = false }
            )
        }

        // ── Grande tuile budget (fixe) ────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1D24))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    homeClickCount++
                    if (homeClickCount >= 10) { showDevMenu = true; homeClickCount = 0 }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                if (activeMode == "solo") Color(0x207C4DFF) else Color(0x202196F3),
                                Color.Transparent
                            ),
                            startX = shimmerOffset * 1000f - 300f,
                            endX = shimmerOffset * 1000f + 300f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    if (activeMode == "solo") "Compte courant" else "Solde total",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${String.format("%.2f", if (activeMode == "solo") soloAccountBalance else totalAll)} €",
                    color = if (activeMode == "solo" && soloAccountBalance < 0) Color(0xFFE53935) else Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                // En solo, cette tuile ne doit refléter QUE le compte courant réel
                // (pas les lignes de charges/épargne, qui sont d'autres comptes) —
                // pas de badges de sous-catégorie à afficher ici.
                if (activeMode != "solo") {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        CategoryBadge("💙", currentTotal, Color(0xFF2196F3))
                        Spacer(modifier = Modifier.weight(1f))
                        CategoryBadge("💚", savingTotal, Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Reste adaptatif ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // ── Tuiles ────────────────────────────────────────
            if (activeMode == "solo") {
                val recurringLines = budgetLines.filter { it.type == "current" }
                val unpaidTotal = recurringLines.filter { !it.paidThisMonth }.sumOf { it.monthlyCost }
                val allPaid = recurringLines.isNotEmpty() && recurringLines.all { it.paidThisMonth }

                val savingLines = budgetLines.filter { it.type == "saving" }
                val savingTotal = savingLines.sumOf { it.monthlyCost }
                val savingPaid = profile?.savingPaidThisMonth == true

                var showSavingConfirm by remember { mutableStateOf(false) }

                if (showSavingConfirm) {
                    AlertDialog(
                        onDismissRequest = { showSavingConfirm = false },
                        containerColor = Color(0xFF1A1D24),
                        title = { Text("Confirmer le virement épargne ?", color = Color.White) },
                        text = { Text("As-tu bien viré ${String.format("%.2f", savingTotal)} € sur tes livrets ?", color = Color.Gray) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.validateSoloSaving()
                                    showSavingConfirm = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) { Text("Oui, c'est fait !", color = Color.White) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSavingConfirm = false }) {
                                Text("Pas encore", color = Color.Gray)
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Tuile charges (non cliquable) ──────────────────
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (allPaid) Color(0xFF0F2F0F) else Color(0xFF1A1D24)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Charges", color = Color.Gray, fontSize = 13.sp)
                            Spacer(Modifier.height(6.dp))
                            if (allPaid) {
                                Text("✓", color = Color(0xFF4CAF50), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    "${String.format("%.2f", unpaidTotal)} €",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Light
                                )
                            }
                        }
                    }

                    // ── Tuile épargne (cliquable) ──────────────────────
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (savingPaid) Color(0xFF0F2F0F) else Color(0xFF1A1D24))
                            .clickable { if (!savingPaid) showSavingConfirm = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Épargne", color = Color.Gray, fontSize = 13.sp)
                            Spacer(Modifier.height(6.dp))
                            if (savingPaid) {
                                Text("✓", color = Color(0xFF4CAF50), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    "${String.format("%.2f", savingTotal)} €",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Light
                                )
                            }
                        }
                    }
                }
            } else {
                val payments = viewModel.calculateMonthlyPayments(budgetLines)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PaymentTile(
                        name = creatorName,
                        amount = payments.first,
                        paid = paymentStatus.creatorPaid,
                        modifier = Modifier.weight(1f),
                        onClick = { pendingPayment = true }
                    )
                    PaymentTile(
                        name = partnerName,
                        amount = payments.second,
                        paid = paymentStatus.partnerPaid,
                        modifier = Modifier.weight(1f),
                        onClick = { pendingPayment = false }
                    )
                }
            }

            // ── Raccourcis ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                shortcuts.forEachIndexed { index, line ->
                    ShortcutTile(
                        line = line,
                        modifier = Modifier.weight(1f),
                        onClick = { showShortcutPicker = index }
                    )
                }
            }

            // ── Graphique ─────────────────────────────────────
            if (balanceSnapshots.size >= 2) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                lines = arrayOf(
                                    rememberLine(
                                        fill = LineCartesianLayer.LineFill.single(fill(Color(0xFF2196F3))),
                                        thickness = 2.dp,
                                        areaFill = LineCartesianLayer.AreaFill.single(
                                            fill(DynamicShader.verticalGradient(
                                                intArrayOf(Color(0x552196F3).toArgb(), Color(0x002196F3).toArgb())
                                            ))
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp))
                )
            }

            // ── Bienvenue ─────────────────────────────────────
            Text(
                "Bienvenue ${profile?.name ?: ""}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp
            )
        }
    }
}

// ── Badge catégorie ───────────────────────────────────────
@Composable
fun CategoryBadge(emoji: String, amount: Double, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 12.sp)
            Text(
                "${String.format("%.2f", amount)} €",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Tuile raccourci ───────────────────────────────────────
@Composable
fun ShortcutTile(
    line: BudgetLine?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (line == null) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter raccourci", tint = Color(0xFF444444), modifier = Modifier.size(24.dp))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(line.title, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${String.format("%.2f", line.remainingAmount)} €",
                    color = if (line.remainingAmount >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Dialog sélection raccourci ────────────────────────────
@Composable
fun ShortcutPickerDialog(
    budgetLines: List<BudgetLine>,
    currentShortcut: BudgetLine?,
    onSelect: (BudgetLine) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1D24),
        title = { Text("Choisir une ligne", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                budgetLines.forEach { line ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentShortcut?.id == line.id) Color(0xFF1A2E3A) else Color(0xFF232323))
                            .clickable { onSelect(line) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(line.title, color = Color.White, fontSize = 14.sp)
                        Text(
                            "${String.format("%.2f", line.remainingAmount)} €",
                            color = if (line.remainingAmount >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentShortcut != null) {
                    TextButton(onClick = onClear) { Text("Retirer", color = Color(0xFFE53935)) }
                }
                TextButton(onClick = onDismiss) { Text("Annuler", color = Color.Gray) }
            }
        }
    )
}

// ── Menu dev caché ────────────────────────────────────────
@Composable
fun DevMenuDialog(viewModel: StartupViewModel, onDismiss: () -> Unit) {
    var countdown by remember { mutableStateOf(5) }
    var countdownStarted by remember { mutableStateOf(false) }
    val activeMode = viewModel.activeMode.collectAsState().value
    val isSolo = activeMode == "solo"

    LaunchedEffect(countdownStarted) {
        if (countdownStarted) {
            while (countdown > 0) { delay(1000); countdown-- }
        }
    }

    AlertDialog(
        onDismissRequest = {},
        containerColor = Color(0xFF1A1D24),
        title = { Text("⚙️ Menu développeur", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Actions de réinitialisation", color = Color.Gray, fontSize = 13.sp)
                Button(
                    onClick = { if (isSolo) viewModel.resetSoloBudgetLines() else viewModel.resetBudgetLines() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F))
                ) { Text("🗂 Reset lignes budgétaires", color = Color.White) }
                Button(
                    onClick = { viewModel.resetHistory() },
                    enabled = !isSolo,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F))
                ) { Text(if (isSolo) "📅 Reset historique (bientôt, solo)" else "📅 Reset historique mensuel", color = Color.White) }
                Button(
                    onClick = { if (isSolo) viewModel.resetSoloTransactions() else viewModel.resetTransactions() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F))
                ) { Text("💸 Reset historique dépenses", color = Color.White) }
                Spacer(Modifier.height(4.dp))
                Text("Zone dangereuse", color = Color(0xFFE53935), fontSize = 13.sp)
                Button(
                    onClick = {
                        if (!countdownStarted) countdownStarted = true
                        else if (countdown == 0) {
                            if (isSolo) viewModel.resetSoloDB() else viewModel.resetDB()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (countdown == 0) Color(0xFFE53935) else Color(0xFF4A1515)
                    )
                ) {
                    Text(
                        text = when {
                            !countdownStarted -> "💣 Reset COMPLET Firestore"
                            countdown > 0 -> "⏳ Attends $countdown secondes..."
                            else -> "⚠️ CONFIRMER LE RESET COMPLET"
                        },
                        color = Color.White
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer", color = Color.Gray) } }
    )
}