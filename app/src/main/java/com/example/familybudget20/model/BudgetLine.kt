package com.example.familybudget20.model

data class BudgetLine(
    val id: String = "",
    val title: String = "",
    val periodicity: String = "monthly", // "monthly" | "quarterly" | "yearly"
    val monthlyCost: Double = 0.0,            // coût ramené au mois
    val baseAmount: Double = 0.0,             // argent déjà présent sur la ligne au départ du mois
    val remainingAmount: Double = 0.0,        // argent restant (évolutif)
    val payer: String = "both",          // "creator" | "partner" | "both"
    val type: String = "current",  // ← "current" | "saving"
    val paidThisMonth: Boolean = false,  //Solo, payé ce mois-ci ?
    val paidTransactionId: String? = null,  // Solo, transaction compte courant liée à la coche
    val createdAt: com.google.firebase.Timestamp? = null
)

