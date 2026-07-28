package com.example.familybudget20.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.familybudget20.model.BudgetLine
import com.example.familybudget20.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.familybudget20.model.PaymentStatus
import com.example.familybudget20.model.Transaction
import kotlin.math.roundToInt

// Additionner des Double représentant des euros dérive petit à petit à cause de
// l'arithmétique binaire (ex: 39.72 n'est pas exactement représentable). On
// arrondit systématiquement au centime après chaque cumul pour éviter des
// valeurs du style 9.8700000000000001.
private fun Double.roundToCents(): Double = (this * 100.0).roundToInt() / 100.0

class StartupViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _startupState = MutableStateFlow<StartupState>(StartupState.Loading)
    val startupState: StateFlow<StartupState> = _startupState

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _budgetLines = MutableStateFlow<List<BudgetLine>>(emptyList())
    val budgetLines: StateFlow<List<BudgetLine>> = _budgetLines

    private val _paymentStatus = MutableStateFlow(PaymentStatus())
    val paymentStatus: StateFlow<PaymentStatus> = _paymentStatus

    private val _partnerProfile = MutableStateFlow<UserProfile?>(null)
    val partnerProfile: StateFlow<UserProfile?> = _partnerProfile

    private val _historyMonths = MutableStateFlow<List<String>>(emptyList())
    val historyMonths: StateFlow<List<String>> = _historyMonths

    private val _historyLines = MutableStateFlow<List<BudgetLine>>(emptyList())
    val historyLines: StateFlow<List<BudgetLine>> = _historyLines

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _activeMode = MutableStateFlow("family") // "family" | "solo"
    val activeMode: StateFlow<String> = _activeMode

    // Statut du compte : anonyme (par défaut) ou lié à un compte Google
    private val _isAnonymousAccount = MutableStateFlow(auth.currentUser?.isAnonymous ?: true)
    val isAnonymousAccount: StateFlow<Boolean> = _isAnonymousAccount

    private val _linkedAccountEmail = MutableStateFlow(auth.currentUser?.email)
    val linkedAccountEmail: StateFlow<String?> = _linkedAccountEmail

    // Listeners liés aux données du mode actif (famille OU solo) : à couper
    // systématiquement avant d'en réattacher, sous peine de les empiler à
    // chaque changement de mode.
    private val modeListeners = mutableListOf<ListenerRegistration>()
    private var userProfileListener: ListenerRegistration? = null
    private var historyMonthsListener: ListenerRegistration? = null

    private fun clearModeListeners() {
        modeListeners.forEach { it.remove() }
        modeListeners.clear()
    }

    override fun onCleared() {
        super.onCleared()
        clearModeListeners()
        userProfileListener?.remove()
        historyMonthsListener?.remove()
    }

    sealed class StartupState {
        object Loading : StartupState()
        object Welcome : StartupState()
        object Onboarding : StartupState()
        object SoloOnboarding : StartupState()  // ← nouveau
        object Home : StartupState()
    }


    init {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user == null) {
                _startupState.value = StartupState.Welcome
                return@launch
            }
            loadUserProfile(user.uid)
        }
    }

    private fun loadUserProfile(uid: String) {
        userProfileListener?.remove()
        userProfileListener = firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("Startup", "Erreur chargement profil utilisateur", error)
                    _startupState.value = StartupState.Onboarding
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    _userProfile.value = profile
                    when {
                        profile?.mode == "solo" -> {
                            _activeMode.value = "solo"
                            loadSoloData(uid)
                        }
                        profile?.familyId?.isNotEmpty() == true -> {
                            _activeMode.value = "family"
                            loadFamily(profile.familyId)
                        }
                        else -> _startupState.value = StartupState.Onboarding
                    }
                } else {
                    // Aucun profil : premier lancement de l'appli, l'utilisateur
                    // doit choisir entre mode famille et mode solo.
                    _startupState.value = StartupState.Welcome
                }
            }
    }

    private fun loadFamily(familyId: String) {
        clearModeListeners()
        val reg = firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("Startup", "Erreur chargement budget famille", error)
                    _startupState.value = StartupState.Onboarding
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    listenToBudgetLines(familyId)
                    listenToPaymentStatus()
                    loadPartnerProfile(familyId)
                    listenToTransactions()
                    listenToBalanceSnapshots()
                    _startupState.value = StartupState.Home
                } else {
                    _startupState.value = StartupState.Onboarding
                }
            }
        modeListeners.add(reg)
    }

    private fun loadPartnerProfile(familyId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val reg = firestore.collection("users")
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val partner = snapshot.documents
                        .mapNotNull { it.toObject(UserProfile::class.java) }
                        .firstOrNull { it.uid != currentUid }
                    _partnerProfile.value = partner
                }
            }
        modeListeners.add(reg)
    }

    private fun listenToBudgetLines(familyId: String) {
        val reg = firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("current")
            .collection("lines")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val lines = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(BudgetLine::class.java)?.copy(id = doc.id)
                    }
                    _budgetLines.value = lines
                }
            }
        modeListeners.add(reg)
    }

    fun listenToPaymentStatus() {
        val familyId = userProfile.value?.familyId ?: return
        val reg = firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("current")
            .collection("payments")
            .document("status")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _paymentStatus.value = PaymentStatus(
                        creatorPaid = snapshot.getBoolean("creatorPaid") ?: false,
                        partnerPaid = snapshot.getBoolean("partnerPaid") ?: false
                    )
                }
            }
        modeListeners.add(reg)
    }

    fun saveUser(
        name: String,
        color: String,
        familyId: String,
        isCreator: Boolean,
        salary: Double   // ← Double
    ) {
        val uid = auth.currentUser?.uid ?: return
        val profile = UserProfile(
            uid = uid,
            name = name,
            color = color,
            familyId = familyId,
            isCreator = isCreator,
            salary = salary
        )
        firestore.collection("users").document(uid).set(profile)
            .addOnFailureListener { e ->
                android.util.Log.e("Onboarding", "Erreur sauvegarde profil utilisateur", e)
            }
    }

    private fun generateFamilyCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun createInitialBudget(familyId: String) {
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val budgetData = mapOf(
            "createdAt" to Timestamp.now(),
            "currentMonth" to currentMonth
        )
        val budgetRef = firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("current")
        budgetRef.set(budgetData)
            .addOnFailureListener { e ->
                android.util.Log.e("Onboarding", "Erreur création budget initial", e)
            }
        budgetRef.collection("payments").document("status")
            .set(mapOf("creatorPaid" to false, "partnerPaid" to false))
            .addOnFailureListener { e ->
                android.util.Log.e("Onboarding", "Erreur création payment status initial", e)
            }
    }

    fun setPaymentStatus(isCreator: Boolean, paid: Boolean) {
        val familyId = userProfile.value?.familyId ?: return
        val paymentsRef = firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("current")
            .collection("payments")
            .document("status")
        val field = if (isCreator) "creatorPaid" else "partnerPaid"
        paymentsRef.update(field, paid)
        applyMonthlyPayments(isCreator)
    }

    fun calculateMonthlyPayments(lines: List<BudgetLine>): Pair<Double, Double> {
        val creatorProfile = if (userProfile.value?.isCreator == true) userProfile.value else partnerProfile.value
        val partnerProfileVal = if (userProfile.value?.isCreator == true) partnerProfile.value else userProfile.value

        val creatorSalary = creatorProfile?.salary ?: 0.0
        val partnerSalary = partnerProfileVal?.salary ?: 0.0

        val totalSalary = creatorSalary + partnerSalary
        val creatorRatio = if (totalSalary > 0) creatorSalary / totalSalary else 0.5
        val partnerRatio = if (totalSalary > 0) partnerSalary / totalSalary else 0.5

        var creatorTotal = 0.0
        var partnerTotal = 0.0

        lines.forEach { line ->
            when (line.payer) {
                "creator" -> creatorTotal += line.monthlyCost
                "partner" -> partnerTotal += line.monthlyCost
                "both" -> {
                    val partnerShare = (line.monthlyCost * partnerRatio * 100).roundToInt() / 100.0
                    val creatorShare = line.monthlyCost - partnerShare
                    creatorTotal += creatorShare
                    partnerTotal += partnerShare
                }
            }
        }

        return Pair(creatorTotal, partnerTotal)
    }

    fun applyMonthlyPayments(isCreator: Boolean) {
        val familyId = userProfile.value?.familyId ?: return
        val linesRef = firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("current")
            .collection("lines")

        linesRef.get().addOnSuccessListener { snapshot ->
            val batch = firestore.batch()

            snapshot.documents.forEach { doc ->
                val line = doc.toObject(BudgetLine::class.java) ?: return@forEach

                val creatorProf = if (userProfile.value?.isCreator == true) userProfile.value else partnerProfile.value
                val partnerProf = if (userProfile.value?.isCreator == true) partnerProfile.value else userProfile.value
                val totalSalary = (creatorProf?.salary ?: 0.0) + (partnerProf?.salary ?: 0.0)
                val creatorRatio = if (totalSalary > 0) (creatorProf?.salary ?: 0.0) / totalSalary else 0.5
                val partnerRatio = if (totalSalary > 0) (partnerProf?.salary ?: 0.0) / totalSalary else 0.5

                val deduction: Double = when (line.payer) {
                    "creator" -> if (isCreator) line.monthlyCost else 0.0
                    "partner" -> if (!isCreator) line.monthlyCost else 0.0
                    "both" -> {
                        val partnerShare = (line.monthlyCost * partnerRatio * 100).roundToInt() / 100.0
                        val creatorShare = line.monthlyCost - partnerShare
                        if (isCreator) creatorShare else partnerShare
                    }
                    else -> 0.0
                }

                if (deduction > 0) {
                    val newRemaining = (line.remainingAmount + deduction).roundToCents()
                    batch.update(doc.reference, "remainingAmount", newRemaining)
                }
            }

            batch.commit()
        }
    }

    fun passToNextMonth() {
        val familyId = userProfile.value?.familyId ?: return
        val currentRef = firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("current")

        currentRef.get().addOnSuccessListener { doc ->
            val oldMonth = doc.getString("currentMonth")
                ?: SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val newMonth = computeNextMonth(oldMonth)
            archiveCurrentMonth(familyId, oldMonth) {
                resetLinesAndPayments(familyId, newMonth)
            }
        }
    }

    private fun computeNextMonth(current: String): String {
        val parts = current.split("-")
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, parts[0].toInt())
        cal.set(Calendar.MONTH, parts[1].toInt() - 1)
        cal.add(Calendar.MONTH, 1)
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    }

    private fun archiveCurrentMonth(familyId: String, oldMonth: String, onComplete: () -> Unit) {
        val budgetsRef = firestore.collection("families")
            .document(familyId)
            .collection("budgets")
        val currentRef = budgetsRef.document("current")

        currentRef.get().addOnSuccessListener { currentBudget ->
            budgetsRef.document("history")
                .set(
                    mapOf("months" to com.google.firebase.firestore.FieldValue.arrayUnion(oldMonth)),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            budgetsRef.document("history")
                .collection(oldMonth)
                .document("meta")
                .set(currentBudget.data ?: mapOf<String, Any>())

            currentRef.collection("lines").get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    val historyLineRef = budgetsRef.document("history")
                        .collection(oldMonth)
                        .document("lines")
                        .collection("items")
                        .document(doc.id)
                    batch.set(historyLineRef, doc.data ?: emptyMap<String, Any>())
                }
                batch.commit().addOnSuccessListener { onComplete() }
            }
        }
    }

    private fun resetLinesAndPayments(familyId: String, newMonth: String) {
        val currentRef = firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("current")

        currentRef.collection("lines").get().addOnSuccessListener { snapshot ->
            val batch = firestore.batch()
            val paymentsRef = currentRef.collection("payments").document("status")
            batch.update(paymentsRef, "creatorPaid", false)
            batch.update(paymentsRef, "partnerPaid", false)
            batch.update(currentRef, "currentMonth", newMonth)
            batch.commit()
        }
    }

    // Nouvelle fonction pour le reset solo
    fun resetSoloMonth() {
        val uid = auth.currentUser?.uid ?: return

        val soloRef = firestore.collection("users")
            .document(uid)
            .collection("solo")
            .document("data")

        soloRef.get().addOnSuccessListener { soloDoc ->
            val currentMonth = soloDoc.getString("currentMonth")
                ?: SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            // Si absent (anciens comptes créés avant ce champ), on part du début des temps
            // pour ne pas faire disparaître des dépenses déjà existantes lors du 1er archivage.
            val periodStart = soloDoc.getTimestamp("currentMonthStartedAt")

            archiveSoloMonth(uid, currentMonth, periodStart) {
                soloRef.collection("lines")
                    .whereEqualTo("type", "current")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val batch = firestore.batch()

                        // Reset paidThisMonth sur toutes les lignes récurrentes. La transaction
                        // du mois précédent reste dans l'historique (vraie dépense passée) :
                        // on efface juste la référence, la coche du nouveau mois repart de zéro.
                        snapshot.documents.forEach { doc ->
                            batch.update(doc.reference, "paidThisMonth", false, "paidTransactionId", null)
                        }

                        // Reset savingPaidThisMonth sur le profil utilisateur
                        val userRef = firestore.collection("users").document(uid)
                        batch.update(userRef, "savingPaidThisMonth", false)

                        // Update du mois courant, à partir du mois stocké (pas de la date réelle)
                        val newMonth = computeNextMonth(currentMonth)
                        batch.update(soloRef, "currentMonth", newMonth, "currentMonthStartedAt", Timestamp.now())

                        batch.commit()
                    }
            }
        }
    }

    //----------------------------------------
    // SOLO : Historique mensuel
    //----------------------------------------
    private val _soloHistoryMonths = MutableStateFlow<List<String>>(emptyList())
    val soloHistoryMonths: StateFlow<List<String>> = _soloHistoryMonths

    // Factures récurrentes payées ce mois-là : titre → montant
    private val _soloHistoryBills = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val soloHistoryBills: StateFlow<List<Pair<String, Double>>> = _soloHistoryBills

    // Gain d'épargne du mois par ligne (pas le total cumulé) : titre → montant
    private val _soloHistorySavings = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val soloHistorySavings: StateFlow<List<Pair<String, Double>>> = _soloHistorySavings

    // Somme des dépenses ponctuelles (hors factures récurrentes) du mois
    private val _soloHistoryExpensesTotal = MutableStateFlow(0.0)
    val soloHistoryExpensesTotal: StateFlow<Double> = _soloHistoryExpensesTotal

    // Archive un instantané du mois avant sa réinitialisation : factures cochées,
    // gain d'épargne (si validé), et total des dépenses ponctuelles (hors 🔁 factures,
    // déjà représentées par les tuiles factures). S'appuie sur les données déjà
    // chargées en mémoire (_budgetLines, _transactions) plutôt que sur une requête
    // Firestore dédiée.
    //
    // Les dépenses sont sélectionnées par une vraie frontière temporelle
    // (periodStart = date du dernier "Nouveau mois"), pas en comparant à la
    // chaîne "currentMonth" : celle-ci n'avance qu'au clic et peut décrocher
    // de la date réelle (ex: plusieurs "Nouveau mois" le même jour en test),
    // ce qui ferait rater les dépenses ajoutées entre-temps.
    private fun archiveSoloMonth(uid: String, oldMonth: String, periodStart: Timestamp?, onComplete: () -> Unit) {
        val soloRef = firestore.collection("users").document(uid).collection("solo").document("data")
        val historyRef = soloRef.collection("history").document("index")

        val startMillis = periodStart?.toDate()?.time ?: 0L
        val expensesTotal = _transactions.value
            .filter {
                it.lineId == "account" && it.type == "depense" &&
                    (it.createdAt?.toDate()?.time ?: 0L) >= startMillis
            }
            .sumOf { it.amount }
            .roundToCents()

        val paidBills = _budgetLines.value.filter { it.type == "current" && it.paidThisMonth }
        val savingsGained = if (userProfile.value?.savingPaidThisMonth == true) {
            _budgetLines.value.filter { it.type == "saving" }
        } else emptyList()

        val batch = firestore.batch()
        batch.set(
            historyRef,
            mapOf("months" to com.google.firebase.firestore.FieldValue.arrayUnion(oldMonth)),
            com.google.firebase.firestore.SetOptions.merge()
        )
        batch.set(
            historyRef.collection(oldMonth).document("meta"),
            mapOf("expensesTotal" to expensesTotal, "archivedAt" to Timestamp.now())
        )
        paidBills.forEach { line ->
            batch.set(
                historyRef.collection(oldMonth).document("bills").collection("items").document(line.id),
                mapOf("title" to line.title, "amount" to line.monthlyCost)
            )
        }
        savingsGained.forEach { line ->
            batch.set(
                historyRef.collection(oldMonth).document("savings").collection("items").document(line.id),
                mapOf("title" to line.title, "amount" to line.monthlyCost)
            )
        }
        batch.commit()
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { e ->
                android.util.Log.e("ArchiveSolo", "Erreur archivage mois $oldMonth", e)
                onComplete()
            }
    }

    fun loadSoloHistoryMonths() {
        val uid = auth.currentUser?.uid ?: return
        historyMonthsListener?.remove()
        historyMonthsListener = firestore.collection("users")
            .document(uid)
            .collection("solo")
            .document("data")
            .collection("history")
            .document("index")
            .addSnapshotListener { snapshot, _ ->
                _soloHistoryMonths.value = if (snapshot != null && snapshot.exists()) {
                    val months = (snapshot.get("months") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    months.sortedDescending().take(24)
                } else emptyList()
            }
    }

    fun loadSoloHistoryDetails(month: String) {
        val uid = auth.currentUser?.uid ?: return
        val monthRef = firestore.collection("users")
            .document(uid)
            .collection("solo")
            .document("data")
            .collection("history")
            .document("index")
            .collection(month)

        monthRef.document("bills").collection("items").get()
            .addOnSuccessListener { snapshot ->
                _soloHistoryBills.value = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val amount = doc.getDouble("amount") ?: return@mapNotNull null
                    title to amount
                }
            }

        monthRef.document("savings").collection("items").get()
            .addOnSuccessListener { snapshot ->
                _soloHistorySavings.value = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val amount = doc.getDouble("amount") ?: return@mapNotNull null
                    title to amount
                }
            }

        monthRef.document("meta").get()
            .addOnSuccessListener { doc ->
                _soloHistoryExpensesTotal.value = doc.getDouble("expensesTotal") ?: 0.0
            }
    }

    fun updateBudgetLine(
        lineId: String,
        title: String,
        periodicity: String,
        rawCost: Double,
        baseAmount: Double,
        payer: String,
        type: String = "current"  // ← nouveau
    ) {
        val uid = auth.currentUser?.uid ?: return
        val linesRef = if (_activeMode.value == "solo") {
            firestore.collection("users").document(uid)
                .collection("solo").document("data").collection("lines")
        } else {
            val familyId = userProfile.value?.familyId ?: return
            firestore.collection("families").document(familyId)
                .collection("budgets").document("current").collection("lines")
        }

        val monthlyCost = when (periodicity) {
            "monthly" -> rawCost
            "quarterly" -> (rawCost / 3 * 100).roundToInt() / 100.0
            "yearly" -> (rawCost / 12 * 100).roundToInt() / 100.0
            else -> rawCost
        }

        // On ne réinitialise jamais remainingAmount ici : c'est le solde évolutif
        // (alimenté par les paiements/virements/dépenses), pas le montant de
        // départ. Mais si l'utilisateur corrige explicitement baseAmount, on
        // répercute la différence sur remainingAmount, sinon l'édition n'a
        // aucun effet visible sur le montant affiché.
        val oldLine = _budgetLines.value.find { it.id == lineId }
        val delta = (baseAmount - (oldLine?.baseAmount ?: baseAmount)).roundToCents()
        val newRemaining = ((oldLine?.remainingAmount ?: baseAmount) + delta).roundToCents()

        val updates = mapOf(
            "title" to title,
            "periodicity" to periodicity,
            "monthlyCost" to monthlyCost,
            "baseAmount" to baseAmount,
            "remainingAmount" to newRemaining,
            "payer" to payer,
            "type" to type   // ← nouveau
        )

        linesRef.document(lineId).update(updates)
    }

    fun deleteBudgetLine(lineId: String) {
        val uid = auth.currentUser?.uid ?: return
        val linesRef = if (_activeMode.value == "solo") {
            firestore.collection("users").document(uid)
                .collection("solo").document("data").collection("lines")
        } else {
            val familyId = userProfile.value?.familyId ?: return
            firestore.collection("families").document(familyId)
                .collection("budgets").document("current").collection("lines")
        }
        linesRef.document(lineId).delete()
    }

    fun joinFamily(code: String, name: String, color: String, salary: Double, onError: (String) -> Unit) {  // ← Double
        firestore.collection("families")
            .document(code.uppercase())
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { onError("Code famille introuvable"); return@addOnSuccessListener }
                saveUser(name = name, color = color, familyId = code.uppercase(), isCreator = false, salary = salary)
            }
            .addOnFailureListener { onError("Erreur de connexion, réessaie") }
    }

    fun loadHistoryMonths() {
        // familyId vaut "" (pas null) en mode solo : il faut l'exclure explicitement,
        // sinon Firestore reçoit une référence de document vide et plante.
        val familyId = userProfile.value?.familyId?.takeIf { it.isNotEmpty() } ?: return
        historyMonthsListener?.remove()
        historyMonthsListener = firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("history")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val months = (snapshot.get("months") as? List<*>)
                        ?.filterIsInstance<String>() ?: emptyList()
                    _historyMonths.value = months.sortedDescending().take(24)
                }
            }
    }

    fun loadHistoryLines(month: String) {
        val familyId = userProfile.value?.familyId?.takeIf { it.isNotEmpty() } ?: return
        firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("history")
            .collection(month)
            .document("lines")
            .collection("items")
            .get()
            .addOnSuccessListener { snapshot ->
                _historyLines.value = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(BudgetLine::class.java)?.copy(id = doc.id)
                }
            }
    }

    fun listenToTransactions() {
        val familyId = userProfile.value?.familyId ?: return
        val reg = firestore.collection("families")
            .document(familyId)
            .collection("transactions")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _transactions.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                    }
                }
            }
        modeListeners.add(reg)
    }

    fun addTransaction(
        line: BudgetLine,
        amount: Double,   // ← Double
        type: String,
        authorName: String,
        annotation: String = ""
    ) {
        val familyId = userProfile.value?.familyId ?: return
        val delta = if (type == "ajout") amount else -amount

        val transaction = hashMapOf(
            "lineId" to line.id,
            "lineTitle" to line.title,
            "amount" to amount,
            "type" to type,
            "authorName" to authorName,
            "annotation" to annotation,
            "createdAt" to Timestamp.now()
        )

        val batch = firestore.batch()
        val transactionRef = firestore.collection("families")
            .document(familyId)
            .collection("transactions")
            .document()
        batch.set(transactionRef, transaction)

        val lineRef = firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("current")
            .collection("lines")
            .document(line.id)
        batch.update(lineRef, "remainingAmount", (line.remainingAmount + delta).roundToCents())
        batch.commit().addOnSuccessListener {
            saveBalanceSnapshot()
        }
    }

    fun resetBudgetLines() {
        val familyId = userProfile.value?.familyId ?: return
        firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("current")
            .collection("lines")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }
    }

    fun resetHistory() {
        val familyId = userProfile.value?.familyId ?: return
        val historyRef = firestore.collection("families")
            .document(familyId)
            .collection("budgets")
            .document("history")

        historyRef.get().addOnSuccessListener { doc ->
            val months = (doc.get("months") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val batch = firestore.batch()
            batch.delete(historyRef)
            batch.commit()
                .addOnSuccessListener {
                    months.forEach { month ->
                        val monthRef = historyRef.collection(month)
                        monthRef.document("lines").collection("items")
                            .get().addOnSuccessListener { itemsSnapshot ->
                                val innerBatch = firestore.batch()
                                itemsSnapshot.documents.forEach { innerBatch.delete(it.reference) }
                                innerBatch.delete(monthRef.document("lines"))
                                innerBatch.delete(monthRef.document("meta"))
                                innerBatch.commit()
                                    .addOnFailureListener { e -> android.util.Log.e("ResetHistory", "Erreur suppression mois $month", e) }
                            }
                            .addOnFailureListener { e -> android.util.Log.e("ResetHistory", "Erreur lecture items mois $month", e) }
                    }
                    _historyMonths.value = emptyList()
                    _historyLines.value = emptyList()
                }
                .addOnFailureListener { e -> android.util.Log.e("ResetHistory", "Erreur suppression doc history", e) }
        }.addOnFailureListener { e -> android.util.Log.e("ResetHistory", "Erreur lecture history", e) }
    }

    fun resetTransactions() {
        val familyId = userProfile.value?.familyId ?: return
        firestore.collection("families")
            .document(familyId)
            .collection("transactions")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }
    }

    fun resetSoloBudgetLines() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .collection("solo").document("data")
            .collection("lines")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }
    }

    fun resetSoloTransactions() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .collection("solo").document("data")
            .collection("transactions")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }
    }

    fun resetSoloHistory() {
        val uid = auth.currentUser?.uid ?: return
        val historyRef = firestore.collection("users").document(uid)
            .collection("solo").document("data")
            .collection("history").document("index")

        historyRef.get().addOnSuccessListener { doc ->
            val months = (doc.get("months") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val batch = firestore.batch()
            batch.delete(historyRef)
            batch.commit()
                .addOnSuccessListener {
                    months.forEach { month ->
                        val monthRef = historyRef.collection(month)
                        monthRef.document("bills").collection("items")
                            .get().addOnSuccessListener { itemsSnapshot ->
                                val innerBatch = firestore.batch()
                                itemsSnapshot.documents.forEach { innerBatch.delete(it.reference) }
                                innerBatch.delete(monthRef.document("bills"))
                                innerBatch.commit()
                                    .addOnFailureListener { e -> android.util.Log.e("ResetSoloHistory", "Erreur suppression factures mois $month", e) }
                            }
                            .addOnFailureListener { e -> android.util.Log.e("ResetSoloHistory", "Erreur lecture factures mois $month", e) }

                        monthRef.document("savings").collection("items")
                            .get().addOnSuccessListener { itemsSnapshot ->
                                val innerBatch = firestore.batch()
                                itemsSnapshot.documents.forEach { innerBatch.delete(it.reference) }
                                innerBatch.delete(monthRef.document("savings"))
                                innerBatch.commit()
                                    .addOnFailureListener { e -> android.util.Log.e("ResetSoloHistory", "Erreur suppression épargne mois $month", e) }
                            }
                            .addOnFailureListener { e -> android.util.Log.e("ResetSoloHistory", "Erreur lecture épargne mois $month", e) }

                        monthRef.document("meta").delete()
                            .addOnFailureListener { e -> android.util.Log.e("ResetSoloHistory", "Erreur suppression meta mois $month", e) }
                    }
                    _soloHistoryMonths.value = emptyList()
                    _soloHistoryBills.value = emptyList()
                    _soloHistorySavings.value = emptyList()
                    _soloHistoryExpensesTotal.value = 0.0
                }
                .addOnFailureListener { e -> android.util.Log.e("ResetSoloHistory", "Erreur suppression doc history", e) }
        }.addOnFailureListener { e -> android.util.Log.e("ResetSoloHistory", "Erreur lecture history", e) }
    }

    fun resetSoloDB() {
        val uid = auth.currentUser?.uid ?: return
        val soloRef = firestore.collection("users").document(uid).collection("solo").document("data")

        soloRef.collection("lines")
            .get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.delete(soloRef.collection("account").document("balance"))
                batch.delete(soloRef)
                batch.commit()
                    .addOnFailureListener { e -> android.util.Log.e("ResetSoloDB", "Erreur suppression lignes/compte", e) }
            }
            .addOnFailureListener { e -> android.util.Log.e("ResetSoloDB", "Erreur lecture lignes", e) }

        soloRef.collection("transactions")
            .get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnFailureListener { e -> android.util.Log.e("ResetSoloDB", "Erreur suppression transactions", e) }
            }
            .addOnFailureListener { e -> android.util.Log.e("ResetSoloDB", "Erreur lecture transactions", e) }

        soloRef.collection("balanceSnapshots")
            .get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnFailureListener { e -> android.util.Log.e("ResetSoloDB", "Erreur suppression balanceSnapshots", e) }
            }
            .addOnFailureListener { e -> android.util.Log.e("ResetSoloDB", "Erreur lecture balanceSnapshots", e) }

        resetSoloHistory()

        firestore.collection("users").document(uid).delete()
            .addOnFailureListener { e -> android.util.Log.e("ResetSoloDB", "Erreur suppression profil", e) }

        _userProfile.value = null
        _budgetLines.value = emptyList()
        _transactions.value = emptyList()
        _historyMonths.value = emptyList()
        _historyLines.value = emptyList()
        _startupState.value = StartupState.Welcome
    }

    fun resetDB() {
        val familyId = userProfile.value?.familyId ?: return
        val familyRef = firestore.collection("families").document(familyId)
        val currentRef = familyRef.collection("budgets").document("current")

        // Lignes + statut paiement + le doc "current" lui-même
        currentRef.collection("lines")
            .get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.delete(currentRef.collection("payments").document("status"))
                batch.delete(currentRef)
                batch.commit()
                    .addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur suppression budget current", e) }
            }
            .addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur lecture lignes", e) }

        familyRef.collection("transactions")
            .get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur suppression transactions", e) }
            }
            .addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur lecture transactions", e) }

        familyRef.collection("balanceSnapshots")
            .get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur suppression balanceSnapshots", e) }
            }
            .addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur lecture balanceSnapshots", e) }

        val historyRef = familyRef.collection("budgets").document("history")
        historyRef.get().addOnSuccessListener { doc ->
            val months = (doc.get("months") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            months.forEach { month ->
                val monthRef = historyRef.collection(month)
                monthRef.document("lines").collection("items")
                    .get().addOnSuccessListener { itemsSnapshot ->
                        val batch = firestore.batch()
                        itemsSnapshot.documents.forEach { batch.delete(it.reference) }
                        batch.delete(monthRef.document("lines"))
                        batch.delete(monthRef.document("meta"))
                        batch.commit()
                            .addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur suppression mois $month", e) }
                    }
                    .addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur lecture items mois $month", e) }
            }
            historyRef.delete()
                .addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur suppression doc history", e) }
        }.addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur lecture history", e) }

        firestore.collection("users")
            .whereEqualTo("familyId", familyId)
            .get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur suppression users", e) }
            }
            .addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur lecture users", e) }

        familyRef.delete()
            .addOnFailureListener { e -> android.util.Log.e("ResetDB", "Erreur suppression doc famille", e) }

        _userProfile.value = null
        _partnerProfile.value = null
        _budgetLines.value = emptyList()
        _transactions.value = emptyList()
        _historyMonths.value = emptyList()
        _historyLines.value = emptyList()
        _startupState.value = StartupState.Welcome
    }

    fun getPayerName(payer: String): String {
        val profile = userProfile.value ?: return ""
        val partner = partnerProfile.value
        val creatorName = if (profile.isCreator) profile.name else partner?.name ?: "Créateur"
        val partnerName = if (profile.isCreator) partner?.name ?: "Partenaire" else profile.name
        return when (payer) {
            "creator" -> creatorName
            "partner" -> partnerName
            "both" -> "Les deux"
            else -> ""
        }
    }

    fun goToOnboarding() { _startupState.value = StartupState.Onboarding }
    fun goToWelcome() { _startupState.value = StartupState.Welcome }

    // Retour à l'accueil depuis l'onboarding famille sans perdre les listeners
    // déjà actifs (cas : utilisateur solo qui a cliqué sur "Famille" par erreur).
    fun cancelOnboarding() { _startupState.value = StartupState.Home }

    //----------------------------------------
    // Graphique homescreen
    //----------------------------------------
    private val _balanceSnapshots = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val balanceSnapshots: StateFlow<List<Pair<String, Double>>> = _balanceSnapshots

    fun listenToBalanceSnapshots() {
        val familyId = userProfile.value?.familyId ?: return

        val reg = firestore.collection("families")
            .document(familyId)
            .collection("balanceSnapshots")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limit(40)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _balanceSnapshots.value = snapshot.documents.mapNotNull { doc ->
                        val date = doc.getString("date") ?: return@mapNotNull null
                        val total = doc.getDouble("total") ?: return@mapNotNull null
                        Pair(date, total)
                    }
                }
            }
        modeListeners.add(reg)
    }

    private fun saveBalanceSnapshot() {
        val familyId = userProfile.value?.familyId ?: return
        val total = _budgetLines.value.sumOf { it.remainingAmount }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        firestore.collection("families")
            .document(familyId)
            .collection("balanceSnapshots")
            .document(today)
            .set(
                mapOf(
                    "date" to today,
                    "total" to total,
                    "createdAt" to Timestamp.now()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
    }
    private fun getBaseRef(): com.google.firebase.firestore.DocumentReference {
        val uid = auth.currentUser?.uid ?: throw Exception("No user")
        return if (_activeMode.value == "solo") {
            firestore.collection("users")
                .document(uid)
                .collection("solo")
                .document("data")
        } else {
            val familyId = userProfile.value?.familyId ?: throw Exception("No family")
            firestore.collection("families")
                .document(familyId)
        }
    }
    fun switchMode(mode: String) {
        _activeMode.value = mode
        // Recharger les données selon le mode
        val uid = auth.currentUser?.uid ?: return
        if (mode == "solo") {
            loadSoloData(uid)
        } else {
            userProfile.value?.familyId?.let { loadFamily(it) }
        }
    }

    private fun loadSoloData(uid: String) {
        clearModeListeners()
        val soloRef = firestore.collection("users")
            .document(uid)
            .collection("solo")
            .document("data")

        // Créer la structure solo si elle n'existe pas
        soloRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                soloRef.set(mapOf(
                    "createdAt" to Timestamp.now(),
                    "currentMonth" to SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
                    "currentMonthStartedAt" to Timestamp.now()
                ))
                soloRef.collection("payments").document("status")
                    .set(mapOf("creatorPaid" to false, "partnerPaid" to false))
            }
            listenToSoloBudgetLines(uid)
            listenToSoloTransactions(uid)
            listenToSoloBalanceSnapshots(uid)
            listenToSoloAccountBalance(uid)
            _startupState.value = StartupState.Home
        }
    }
    private fun listenToSoloBudgetLines(uid: String) {
        val reg = firestore.collection("users")
            .document(uid)
            .collection("solo")
            .document("data")
            .collection("lines")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val lines = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(BudgetLine::class.java)?.copy(id = doc.id)
                    }
                    _budgetLines.value = lines
                }
            }
        modeListeners.add(reg)
    }

    private fun listenToSoloTransactions(uid: String) {
        val reg = firestore.collection("users")
            .document(uid)
            .collection("solo")
            .document("data")
            .collection("transactions")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _transactions.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                    }
                }
            }
        modeListeners.add(reg)
    }

    private fun listenToSoloBalanceSnapshots(uid: String) {
        val reg = firestore.collection("users")
            .document(uid)
            .collection("solo")
            .document("data")
            .collection("balanceSnapshots")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limit(40)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _balanceSnapshots.value = snapshot.documents.mapNotNull { doc ->
                        val date = doc.getString("date") ?: return@mapNotNull null
                        val total = doc.getDouble("total") ?: return@mapNotNull null
                        Pair(date, total)
                    }
                }
            }
        modeListeners.add(reg)
    }
    fun addBudgetLine(title: String, periodicity: String, rawCost: Double, baseAmount: Double, payer: String, type: String = "current") {
        val uid = auth.currentUser?.uid ?: return
        val linesRef = if (_activeMode.value == "solo") {
            firestore.collection("users").document(uid)
                .collection("solo").document("data").collection("lines")
        } else {
            val familyId = userProfile.value?.familyId ?: return
            firestore.collection("families").document(familyId)
                .collection("budgets").document("current").collection("lines")
        }

        val monthlyCost = when (periodicity) {
            "monthly" -> rawCost
            "quarterly" -> (rawCost / 3 * 100).roundToInt() / 100.0
            "yearly" -> (rawCost / 12 * 100).roundToInt() / 100.0
            else -> rawCost
        }

        val line = mapOf(
            "title" to title,
            "periodicity" to periodicity,
            "monthlyCost" to monthlyCost,
            "baseAmount" to baseAmount,
            "remainingAmount" to baseAmount,
            "payer" to payer,
            "type" to type
        )

        linesRef.add(line)
    }


// SOLO : Fonction du payement des "dettes"
    fun toggleLinePaid(lineId: String, paid: Boolean) {
        val uid = auth.currentUser?.uid ?: return

        if (_activeMode.value != "solo") {
            val familyId = userProfile.value?.familyId ?: return
            firestore.collection("families").document(familyId)
                .collection("budgets").document("current").collection("lines")
                .document(lineId).update("paidThisMonth", paid)
            return
        }

        // En solo, cocher/décocher une charge récurrente doit aussi débiter/recréditer
        // le compte courant, pour que le solde affiché colle à la réalité bancaire
        // (la charge est réellement prélevée par la banque, juste pas manuellement).
        val line = _budgetLines.value.firstOrNull { it.id == lineId } ?: return
        val soloRef = firestore.collection("users").document(uid).collection("solo").document("data")
        val lineRef = soloRef.collection("lines").document(lineId)
        val balanceRef = soloRef.collection("account").document("balance")
        val batch = firestore.batch()

        if (paid) {
            val transactionRef = soloRef.collection("transactions").document()
            batch.set(
                transactionRef,
                hashMapOf(
                    "lineId" to lineId,
                    "lineTitle" to line.title,
                    "amount" to line.monthlyCost,
                    "type" to "depense",
                    "authorName" to (userProfile.value?.name ?: ""),
                    "annotation" to "🔁",
                    "createdAt" to Timestamp.now()
                )
            )
            batch.update(lineRef, "paidThisMonth", true, "paidTransactionId", transactionRef.id)
            batch.set(
                balanceRef,
                mapOf(
                    "balance" to (_soloAccountBalance.value - line.monthlyCost).roundToCents(),
                    "updatedAt" to Timestamp.now()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        } else {
            batch.update(lineRef, "paidThisMonth", false, "paidTransactionId", null)
            val transactionId = line.paidTransactionId
            if (transactionId != null) {
                batch.delete(soloRef.collection("transactions").document(transactionId))
                batch.set(
                    balanceRef,
                    mapOf(
                        "balance" to (_soloAccountBalance.value + line.monthlyCost).roundToCents(),
                        "updatedAt" to Timestamp.now()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }
        }

        batch.commit()
    }

    // SOLO : Fonction du payement des Epargnes
    fun getSoloMonthlySaving(): Double {
        return _budgetLines.value
            .filter { it.type == "saving" }
            .sumOf { it.monthlyCost }
    }

    fun validateSoloSaving() {
        val uid = auth.currentUser?.uid ?: return
        val lines = _budgetLines.value.filter { it.type == "saving" }
        val soloRef = firestore.collection("users")
            .document(uid)
            .collection("solo")
            .document("data")

        val batch = firestore.batch()  // ← déclaré UNE FOIS en dehors du forEach

        lines.forEach { line ->
            val lineRef = soloRef.collection("lines").document(line.id)
            batch.update(lineRef, "remainingAmount", (line.remainingAmount + line.monthlyCost).roundToCents())
        }

        val userRef = firestore.collection("users").document(uid)
        batch.update(userRef, "savingPaidThisMonth", true)

        batch.commit()
    }
    //----------------------------------------
    // Solo transaction
    //----------------------------------------
    fun addSoloTransaction(
        amount: Double,
        type: String,
        category: com.example.familybudget20.model.TransactionCategory
    ) {
        val uid = auth.currentUser?.uid ?: return
        val delta = if (type == "ajout") amount else -amount

        val transaction = hashMapOf(
            "lineId" to "account",
            "lineTitle" to category.label,
            "amount" to amount,
            "type" to type,
            "authorName" to (userProfile.value?.name ?: ""),
            "annotation" to category.emoji,
            "createdAt" to Timestamp.now()
        )

        val soloRef = firestore.collection("users")
            .document(uid)
            .collection("solo")
            .document("data")

        val batch = firestore.batch()

        // Ajouter la transaction
        val transactionRef = soloRef.collection("transactions").document()
        batch.set(transactionRef, transaction)

        // Mettre à jour le solde depuis la valeur actuelle du StateFlow
        val newBalance = (_soloAccountBalance.value + delta).roundToCents()  // ← utilise le vrai solde
        batch.set(
            soloRef.collection("account").document("balance"),
            mapOf(
                "balance" to newBalance,
                "updatedAt" to Timestamp.now()
            )
        )

        batch.commit()
    }
    private val _soloAccountBalance = MutableStateFlow(0.0)
    val soloAccountBalance: StateFlow<Double> = _soloAccountBalance

    private fun listenToSoloAccountBalance(uid: String) {
        val reg = firestore.collection("users")
            .document(uid)
            .collection("solo")
            .document("data")
            .collection("account")
            .document("balance")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _soloAccountBalance.value = snapshot.getDouble("balance") ?: 0.0
                } else {
                    // Le document balance a été supprimé (reset manuel, purge partielle...) :
                    // on ne doit pas garder l'ancienne valeur en mémoire.
                    _soloAccountBalance.value = 0.0
                }
            }
        modeListeners.add(reg)
    }
    fun setSoloAccountBalance(balance: Double) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .collection("solo")
            .document("data")
            .collection("account")
            .document("balance")
            .set(mapOf(
                "balance" to balance,
                "updatedAt" to Timestamp.now()
            ))
    }

    fun goToSoloOnboarding() {
        _startupState.value = StartupState.SoloOnboarding
    }
    fun finishOnboarding(
        name: String,
        color: String,
        salary: Double,
        onError: (String) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: return
        val familyId = generateFamilyCode()

        val familyData = mapOf(
            "familyId" to familyId,
            "owner" to uid,
            "createdAt" to Timestamp.now()
        )

        firestore.collection("families")
            .document(familyId)
            .set(familyData)
            .addOnSuccessListener {
                createInitialBudget(familyId)
                saveUser(
                    name = name,
                    color = color,
                    familyId = familyId,
                    isCreator = true,
                    salary = salary
                )
            }
            .addOnFailureListener { e ->
                android.util.Log.e("Onboarding", "Erreur création famille", e)
                onError(e.message ?: "Erreur lors de la création de la famille")
            }
    }
    fun finishSoloOnboarding(
        name: String,
        color: String,
        salary: Double,
        initialBalance: Double
    ) {
        val uid = auth.currentUser?.uid ?: return

        val profile = UserProfile(
            uid = uid,
            name = name,
            color = color,
            familyId = "",
            isCreator = false,
            salary = salary,
            mode = "solo"
        )
        firestore.collection("users").document(uid).set(profile)

        val soloRef = firestore.collection("users")
            .document(uid)
            .collection("solo")
            .document("data")

        soloRef.set(mapOf(
            "createdAt" to Timestamp.now(),
            "currentMonth" to SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
            "currentMonthStartedAt" to Timestamp.now()
        ))

        soloRef.collection("account").document("balance")
            .set(mapOf(
                "balance" to initialBalance,
                "updatedAt" to Timestamp.now()
            ))

        _activeMode.value = "solo"
        _startupState.value = StartupState.Home
    }

    // Lie le compte Google à l'utilisateur anonyme courant (même UID, aucune
    // migration de données). Si ce compte Google est déjà lié à un AUTRE UID
    // (cas "nouveau téléphone, je récupère mes données"), on bascule sur ce
    // compte existant à la place et on recharge tout l'état de l'app.
    fun linkOrRecoverGoogleAccount(idToken: String, onResult: (success: Boolean, message: String) -> Unit) {
        val current = auth.currentUser
        if (current == null || !current.isAnonymous) {
            onResult(false, "Aucun compte anonyme actif à lier")
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        current.linkWithCredential(credential)
            .addOnSuccessListener { result ->
                _isAnonymousAccount.value = false
                _linkedAccountEmail.value = result.user?.email
                onResult(true, "Compte Google lié avec succès")
            }
            .addOnFailureListener { e ->
                if (e is FirebaseAuthUserCollisionException) {
                    recoverExistingGoogleAccount(credential, onResult)
                } else {
                    android.util.Log.e("Startup", "Erreur liaison compte Google", e)
                    onResult(false, e.message ?: "Erreur lors de la liaison du compte Google")
                }
            }
    }

    private fun recoverExistingGoogleAccount(
        credential: com.google.firebase.auth.AuthCredential,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    onResult(false, "Erreur lors de la récupération du compte")
                    return@addOnSuccessListener
                }
                _isAnonymousAccount.value = result.user?.isAnonymous ?: false
                _linkedAccountEmail.value = result.user?.email

                clearModeListeners()
                userProfileListener?.remove()
                historyMonthsListener?.remove()
                _startupState.value = StartupState.Loading
                loadUserProfile(uid)

                onResult(true, "Compte existant récupéré")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("Startup", "Erreur récupération compte Google existant", e)
                onResult(false, e.message ?: "Impossible de récupérer le compte existant")
            }
    }
}