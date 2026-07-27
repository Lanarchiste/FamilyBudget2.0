package com.example.familybudget20.model

data class TransactionCategory(
    val emoji: String,
    val label: String
)

val transactionCategories = listOf(
    TransactionCategory("🚗", "Voiture"),
    TransactionCategory("⛽", "Carburant"),
    TransactionCategory("🏠", "Maison"),
    TransactionCategory("🛒", "Courses"),
    TransactionCategory("👕", "Vêtements"),
    TransactionCategory("🍔", "Resto"),
    TransactionCategory("💊", "Santé"),
    TransactionCategory("✈️", "Voyage"),
    TransactionCategory("🎮", "Loisirs"),
    TransactionCategory("💰", "Salaire"),
    TransactionCategory("📈", "Dividendes"),
    TransactionCategory("🔧", "Travail"),
    TransactionCategory("🤝", "Remboursement"),
    TransactionCategory("📱", "Abonnement"),
    TransactionCategory("🎓", "Formation"),
    TransactionCategory("🐾", "Animaux"),
    TransactionCategory("🎁", "Cadeaux"),
    TransactionCategory("⚡", "Énergie"),
    TransactionCategory("🏋️", "Sport"),
    TransactionCategory("🚌", "Transport"),
    TransactionCategory("💇", "Beauté"),
    TransactionCategory("🍺", "Sorties"),
    TransactionCategory("📚", "Livres"),
    TransactionCategory("🖥️", "Tech"),
    TransactionCategory("❓", "Autre")
)